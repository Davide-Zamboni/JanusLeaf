package com.janusleaf.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.janusleaf.app.domain.model.JournalResult
import com.janusleaf.app.viewmodel.state.JournalEditorUiState
import com.janusleaf.app.model.data.store.JournalStore
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class JournalEditorViewModel(
    private val journalStore: JournalStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(JournalEditorUiState())
    val uiState: StateFlow<JournalEditorUiState> = _uiState.asStateFlow()

    private var currentEntryId: String? = null
    private var entryObserverJob: Job? = null
    private var autoSaveJob: Job? = null
    private var pendingBodyUpdate: String? = null
    private var currentVersion: Long? = null

    fun bindEntry(entryId: String) {
        if (currentEntryId == entryId) return
        currentEntryId = entryId
        pendingBodyUpdate = null
        autoSaveJob?.cancel()
        entryObserverJob?.cancel()
        entryObserverJob = viewModelScope.launch {
            journalStore.observeEntry(entryId).collect { entry ->
                currentVersion = entry?.version
                _uiState.update { it.copy(entry = entry) }
            }
        }
    }

    fun loadEntry(entryId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = journalStore.getEntry(entryId)) {
                is JournalResult.Success -> {
                    currentVersion = result.data.version
                    _uiState.update { it.copy(isLoading = false) }
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

    fun updateBody(entryId: String, body: String) {
        pendingBodyUpdate = body
        autoSaveJob?.cancel()
        autoSaveJob = viewModelScope.launch {
            delay(1500)
            performAutoSave(entryId)
        }
    }

    fun updateTitle(entryId: String, title: String, onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            when (val result = journalStore.updateMetadata(entryId, title = title, expectedVersion = currentVersion)) {
                is JournalResult.Success -> {
                    currentVersion = result.data.version
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
            when (val result = journalStore.updateMetadata(entryId, moodScore = score, expectedVersion = currentVersion)) {
                is JournalResult.Success -> {
                    currentVersion = result.data.version
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

    fun forceSave(entryId: String, onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            autoSaveJob?.cancel()
            val body = pendingBodyUpdate ?: _uiState.value.entry?.body
            if (body == null) {
                onComplete(true)
                return@launch
            }
            _uiState.update { it.copy(isSaving = true) }
            when (val result = journalStore.updateBody(entryId, body, currentVersion)) {
                is JournalResult.Success -> {
                    currentVersion = result.data.version
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            lastSavedAtEpochMillis = result.data.updatedAt.toEpochMilliseconds()
                        )
                    }
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

    fun deleteEntry(entryId: String, onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            when (val result = journalStore.deleteEntry(entryId)) {
                is JournalResult.Success -> {
                    _uiState.update { it.copy(isLoading = false, entry = null) }
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

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private suspend fun performAutoSave(entryId: String) {
        val body = pendingBodyUpdate ?: return
        _uiState.update { it.copy(isSaving = true) }
        when (val result = journalStore.updateBody(entryId, body, currentVersion)) {
            is JournalResult.Success -> {
                currentVersion = result.data.version
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        lastSavedAtEpochMillis = result.data.updatedAt.toEpochMilliseconds()
                    )
                }
                pendingBodyUpdate = null
            }
            is JournalResult.Error -> {
                _uiState.update { it.copy(isSaving = false, errorMessage = result.error.toUserMessage()) }
            }
            is JournalResult.Loading -> Unit
        }
    }

    override fun onCleared() {
        autoSaveJob?.cancel()
        entryObserverJob?.cancel()
        super.onCleared()
    }
}
