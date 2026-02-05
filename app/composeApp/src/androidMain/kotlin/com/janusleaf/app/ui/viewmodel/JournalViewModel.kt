package com.janusleaf.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.janusleaf.app.domain.model.Journal
import com.janusleaf.app.domain.model.JournalPreview
import com.janusleaf.app.domain.model.JournalResult
import com.janusleaf.app.domain.repository.JournalRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

private const val PAGE_SIZE = 20

 data class JournalUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val entries: List<JournalPreview> = emptyList(),
    val currentEntry: Journal? = null,
    val hasMore: Boolean = true,
    val isSaving: Boolean = false,
    val lastSavedAtEpochMillis: Long? = null
)

class JournalViewModel(
    private val journalRepository: JournalRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(JournalUiState())
    val uiState: StateFlow<JournalUiState> = _uiState.asStateFlow()

    private var currentPage = 0
    private var isLoadingMore = false
    private var autoSaveJob: Job? = null
    private var pendingBodyUpdate: String? = null
    private var currentVersion: Long? = null

    fun loadEntries() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            currentPage = 0
            when (val result = journalRepository.listEntries(page = 0, size = PAGE_SIZE)) {
                is JournalResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            entries = result.data.entries,
                            hasMore = result.data.hasNext
                        )
                    }
                }
                is JournalResult.Error -> {
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = result.error.toUserMessage())
                    }
                }
                is JournalResult.Loading -> Unit
            }
        }
    }

    fun loadMoreEntries() {
        if (isLoadingMore || !_uiState.value.hasMore) return
        viewModelScope.launch {
            isLoadingMore = true
            when (val result = journalRepository.listEntries(page = currentPage + 1, size = PAGE_SIZE)) {
                is JournalResult.Success -> {
                    currentPage = result.data.page
                    _uiState.update {
                        it.copy(
                            entries = it.entries + result.data.entries,
                            hasMore = result.data.hasNext
                        )
                    }
                }
                is JournalResult.Error -> {
                    _uiState.update { it.copy(errorMessage = result.error.toUserMessage()) }
                }
                is JournalResult.Loading -> Unit
            }
            isLoadingMore = false
        }
    }

    fun refresh() {
        loadEntries()
    }

    fun createEntry(
        title: String? = null,
        body: String? = null,
        entryDate: LocalDate? = null,
        onComplete: (Journal?) -> Unit = {}
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val resolvedDate = entryDate ?: Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
            when (val result = journalRepository.createEntry(title, body, resolvedDate)) {
                is JournalResult.Success -> {
                    val preview = JournalPreview(
                        id = result.data.id,
                        title = result.data.title,
                        bodyPreview = result.data.body.take(150),
                        moodScore = result.data.moodScore,
                        entryDate = result.data.entryDate,
                        updatedAt = result.data.updatedAt
                    )
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            entries = listOf(preview) + it.entries,
                            currentEntry = result.data
                        )
                    }
                    onComplete(result.data)
                }
                is JournalResult.Error -> {
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = result.error.toUserMessage())
                    }
                    onComplete(null)
                }
                is JournalResult.Loading -> Unit
            }
        }
    }

    fun getEntry(id: String, onComplete: (Journal?) -> Unit = {}) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = journalRepository.getEntry(id)) {
                is JournalResult.Success -> {
                    currentVersion = result.data.version
                    _uiState.update { it.copy(isLoading = false, currentEntry = result.data) }
                    syncEntryPreview(result.data)
                    onComplete(result.data)
                }
                is JournalResult.Error -> {
                    _uiState.update { it.copy(isLoading = false, errorMessage = result.error.toUserMessage()) }
                    onComplete(null)
                }
                is JournalResult.Loading -> Unit
            }
        }
    }

    fun updateBody(entryId: String, body: String) {
        pendingBodyUpdate = body
        autoSaveJob?.cancel()
        autoSaveJob = viewModelScope.launch {
            delay(1500)
            performAutoSave(entryId)
        }
    }

    private suspend fun performAutoSave(entryId: String) {
        val body = pendingBodyUpdate ?: return
        _uiState.update { it.copy(isSaving = true) }
        when (val result = journalRepository.updateBody(entryId, body, currentVersion)) {
            is JournalResult.Success -> {
                currentVersion = result.data.version
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        lastSavedAtEpochMillis = result.data.updatedAt.toEpochMilliseconds()
                    )
                }
                _uiState.update { state ->
                    val current = state.currentEntry
                    if (current != null && current.id == entryId) {
                        state.copy(
                            currentEntry = current.copy(
                                body = result.data.body,
                                version = result.data.version,
                                updatedAt = result.data.updatedAt
                            )
                        )
                    } else {
                        state
                    }
                }
                updateEntryPreview(entryId, body.take(150))
                pendingBodyUpdate = null
            }
            is JournalResult.Error -> {
                _uiState.update { it.copy(isSaving = false, errorMessage = result.error.toUserMessage()) }
            }
            is JournalResult.Loading -> Unit
        }
    }

    fun forceSave(entryId: String, onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            autoSaveJob?.cancel()
            val body = pendingBodyUpdate ?: _uiState.value.currentEntry?.body
            if (body == null) {
                onComplete(true)
                return@launch
            }
            _uiState.update { it.copy(isSaving = true) }
            when (val result = journalRepository.updateBody(entryId, body, currentVersion)) {
            is JournalResult.Success -> {
                currentVersion = result.data.version
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        lastSavedAtEpochMillis = result.data.updatedAt.toEpochMilliseconds()
                    )
                }
                _uiState.update { state ->
                    val current = state.currentEntry
                    if (current != null && current.id == entryId) {
                        state.copy(
                            currentEntry = current.copy(
                                body = result.data.body,
                                version = result.data.version,
                                updatedAt = result.data.updatedAt
                            )
                        )
                    } else {
                        state
                    }
                }
                updateEntryPreview(entryId, body.take(150))
                pendingBodyUpdate = null
                onComplete(true)
            }
                is JournalResult.Error -> {
                    _uiState.update { it.copy(isSaving = false, errorMessage = result.error.toUserMessage()) }
                    onComplete(false)
                }
                is JournalResult.Loading -> Unit
            }
        }
    }

    fun updateTitle(entryId: String, title: String, onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            when (val result = journalRepository.updateMetadata(entryId, title, null, currentVersion)) {
                is JournalResult.Success -> {
                    currentVersion = result.data.version
                    _uiState.update { it.copy(currentEntry = result.data) }
                    syncEntryPreview(result.data)
                    onComplete(true)
                }
                is JournalResult.Error -> {
                    _uiState.update { it.copy(errorMessage = result.error.toUserMessage()) }
                    onComplete(false)
                }
                is JournalResult.Loading -> Unit
            }
        }
    }

    fun updateMoodScore(entryId: String, score: Int, onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            when (val result = journalRepository.updateMetadata(entryId, null, score, currentVersion)) {
                is JournalResult.Success -> {
                    currentVersion = result.data.version
                    _uiState.update { it.copy(currentEntry = result.data) }
                    syncEntryPreview(result.data)
                    onComplete(true)
                }
                is JournalResult.Error -> {
                    _uiState.update { it.copy(errorMessage = result.error.toUserMessage()) }
                    onComplete(false)
                }
                is JournalResult.Loading -> Unit
            }
        }
    }

    fun deleteEntry(entryId: String, onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            when (val result = journalRepository.deleteEntry(entryId)) {
                is JournalResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            entries = it.entries.filterNot { entry -> entry.id == entryId },
                            currentEntry = if (it.currentEntry?.id == entryId) null else it.currentEntry
                        )
                    }
                    onComplete(true)
                }
                is JournalResult.Error -> {
                    _uiState.update { it.copy(isLoading = false, errorMessage = result.error.toUserMessage()) }
                    onComplete(false)
                }
                is JournalResult.Loading -> Unit
            }
        }
    }

    fun clearCurrentEntry() {
        autoSaveJob?.cancel()
        pendingBodyUpdate = null
        currentVersion = null
        _uiState.update { it.copy(currentEntry = null) }
    }

    fun clearAll() {
        autoSaveJob?.cancel()
        pendingBodyUpdate = null
        currentVersion = null
        currentPage = 0
        _uiState.update { it.copy(entries = emptyList(), currentEntry = null, hasMore = true, errorMessage = null) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private fun updateEntryPreview(entryId: String, bodyPreview: String) {
        _uiState.update { state ->
            val updatedEntries = state.entries.map { entry ->
                if (entry.id == entryId) {
                    entry.copy(bodyPreview = bodyPreview, moodScore = null)
                } else {
                    entry
                }
            }
            state.copy(entries = updatedEntries)
        }
    }

    private fun syncEntryPreview(journal: Journal) {
        _uiState.update { state ->
            val updatedEntries = state.entries.map { entry ->
                if (entry.id == journal.id) {
                    JournalPreview(
                        id = journal.id,
                        title = journal.title,
                        bodyPreview = journal.body.take(150),
                        moodScore = journal.moodScore,
                        entryDate = journal.entryDate,
                        updatedAt = journal.updatedAt
                    )
                } else {
                    entry
                }
            }
            state.copy(entries = updatedEntries)
        }
    }
}
