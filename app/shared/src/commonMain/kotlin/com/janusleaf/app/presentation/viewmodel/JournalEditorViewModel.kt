package com.janusleaf.app.presentation.viewmodel

import com.janusleaf.app.domain.model.Journal
import com.janusleaf.app.domain.model.JournalResult
import com.janusleaf.app.model.store.JournalStore
import com.janusleaf.app.presentation.state.JournalEditorUiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class JournalEditorViewModel(
    private val journalStore: JournalStore
) : KmpViewModel() {

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
        entryObserverJob = launchSafely(
            operation = "bindEntry",
            onError = {
                _uiState.update { state ->
                    state.copy(errorMessage = "Unable to observe this entry right now.")
                }
            }
        ) {
            journalStore.observeEntry(entryId).collect { entry ->
                currentVersion = entry?.version
                _uiState.update { it.copy(entry = entry) }
            }
        }
    }

    fun clearBoundEntry() {
        currentEntryId = null
        pendingBodyUpdate = null
        autoSaveJob?.cancel()
        entryObserverJob?.cancel()
        currentVersion = null
        _uiState.update {
            it.copy(
                entry = null,
                isLoading = false,
                isSaving = false
            )
        }
    }

    fun loadEntry(entryId: String) {
        loadEntry(entryId, onSuccess = null, onError = null)
    }

    fun loadEntry(
        entryId: String,
        onSuccess: ((Journal) -> Unit)?,
        onError: ((String) -> Unit)?
    ) {
        launchSafely(
            operation = "loadEntry",
            onError = { throwable ->
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = "Unable to load this entry right now.")
                }
                onError?.invoke(throwable.message ?: "Unable to load this entry right now.")
            }
        ) {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = journalStore.getEntry(entryId)) {
                is JournalResult.Success -> {
                    currentVersion = result.data.version
                    _uiState.update { it.copy(isLoading = false) }
                    onSuccess?.invoke(result.data)
                }

                is JournalResult.Error -> {
                    val message = result.error.toUserMessage()
                    _uiState.update { it.copy(isLoading = false, errorMessage = message) }
                    onError?.invoke(message)
                }

                is JournalResult.Loading -> Unit
            }
        }
    }

    fun updateBody(entryId: String, body: String) {
        pendingBodyUpdate = body
        autoSaveJob?.cancel()
        autoSaveJob = launchSafely(operation = "queueBodyUpdate") {
            delay(1_500)
            performAutoSave(entryId)
        }
    }

    fun updateBody(
        id: String,
        body: String,
        expectedVersion: Long?,
        onSuccess: (Long) -> Unit,
        onError: (String) -> Unit
    ) {
        launchSafely(
            operation = "updateBody",
            onError = {
                _uiState.update { state ->
                    state.copy(isSaving = false, errorMessage = "Unable to save your entry right now.")
                }
                onError("Unable to save your entry right now.")
            }
        ) {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            when (
                val result = journalStore.updateBody(
                    id = id,
                    body = body,
                    expectedVersion = expectedVersion ?: currentVersion
                )
            ) {
                is JournalResult.Success -> {
                    currentVersion = result.data.version
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            lastSavedAtEpochMillis = result.data.updatedAt.toEpochMilliseconds()
                        )
                    }
                    pendingBodyUpdate = null
                    onSuccess(result.data.version)
                }

                is JournalResult.Error -> {
                    val message = result.error.toUserMessage()
                    _uiState.update { it.copy(isSaving = false, errorMessage = message) }
                    onError(message)
                }

                is JournalResult.Loading -> Unit
            }
        }
    }

    fun updateTitle(entryId: String, title: String, onComplete: (Boolean) -> Unit = {}) {
        updateMetadata(
            id = entryId,
            title = title,
            moodScore = null,
            expectedVersion = currentVersion,
            onSuccess = {
                currentVersion = it.version
                onComplete(true)
            },
            onError = {
                onComplete(false)
            }
        )
    }

    fun updateMoodScore(entryId: String, score: Int, onComplete: (Boolean) -> Unit = {}) {
        updateMetadata(
            id = entryId,
            title = null,
            moodScore = score,
            expectedVersion = currentVersion,
            onSuccess = {
                currentVersion = it.version
                onComplete(true)
            },
            onError = {
                onComplete(false)
            }
        )
    }

    fun updateMetadata(
        id: String,
        title: String?,
        moodScore: Int?,
        expectedVersion: Long?,
        onSuccess: (Journal) -> Unit,
        onError: (String) -> Unit
    ) {
        launchSafely(
            operation = "updateMetadata",
            onError = {
                _uiState.update { state ->
                    state.copy(errorMessage = "Unable to save entry details right now.")
                }
                onError("Unable to save entry details right now.")
            }
        ) {
            when (val result = journalStore.updateMetadata(id, title, moodScore, expectedVersion)) {
                is JournalResult.Success -> {
                    currentVersion = result.data.version
                    onSuccess(result.data)
                }

                is JournalResult.Error -> {
                    val message = result.error.toUserMessage()
                    _uiState.update { it.copy(errorMessage = message) }
                    onError(message)
                }

                is JournalResult.Loading -> Unit
            }
        }
    }

    fun forceSave(entryId: String, onComplete: (Boolean) -> Unit = {}) {
        launchSafely(
            operation = "forceSave",
            onError = {
                _uiState.update { state ->
                    state.copy(isSaving = false, errorMessage = "Unable to save your entry right now.")
                }
                onComplete(false)
            }
        ) {
            autoSaveJob?.cancel()
            val body = pendingBodyUpdate ?: _uiState.value.entry?.body
            if (body == null) {
                onComplete(true)
                return@launchSafely
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
        launchSafely(
            operation = "deleteEntry",
            onError = {
                _uiState.update { state ->
                    state.copy(isLoading = false, errorMessage = "Unable to delete this entry right now.")
                }
                onComplete(false)
            }
        ) {
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
    }
}
