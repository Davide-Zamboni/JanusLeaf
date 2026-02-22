package com.janusleaf.app.presentation.viewmodel

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
                isSaving = false,
                pendingNavigateBack = false
            )
        }
    }

    fun loadEntry(entryId: String) {
        launchSafely(
            operation = "loadEntry",
            onError = {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = "Unable to load this entry right now.")
                }
            }
        ) {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = journalStore.getEntry(entryId)) {
                is JournalResult.Success -> {
                    currentVersion = result.data.version
                    _uiState.update { it.copy(isLoading = false) }
                }

                is JournalResult.Error -> {
                    val message = result.error.toUserMessage()
                    _uiState.update { it.copy(isLoading = false, errorMessage = message) }
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

    fun updateTitle(entryId: String, title: String) {
        if (title.isBlank()) return
        launchSafely(
            operation = "updateTitle",
            onError = {
                _uiState.update { state ->
                    state.copy(errorMessage = "Unable to save entry details right now.")
                }
            }
        ) {
            updateMetadataInternal(
                id = entryId,
                title = title,
                moodScore = null,
                expectedVersion = currentVersion
            )
        }
    }

    fun updateMoodScore(entryId: String, score: Int) {
        launchSafely(
            operation = "updateMoodScore",
            onError = {
                _uiState.update { state ->
                    state.copy(errorMessage = "Unable to save entry details right now.")
                }
            }
        ) {
            updateMetadataInternal(
                id = entryId,
                title = null,
                moodScore = score,
                expectedVersion = currentVersion
            )
        }
    }

    fun forceSave(entryId: String) {
        launchSafely(
            operation = "forceSave",
            onError = {
                _uiState.update { state ->
                    state.copy(isSaving = false, errorMessage = "Unable to save your entry right now.")
                }
            }
        ) {
            forceSaveInternal(entryId)
        }
    }

    fun requestClose(entryId: String, draftTitle: String) {
        launchSafely(
            operation = "requestClose",
            onError = {
                _uiState.update { state ->
                    state.copy(isSaving = false, errorMessage = "Unable to save your entry right now.")
                }
            }
        ) {
            val persistedTitle = _uiState.value.entry?.title.orEmpty()
            val hasTitleChanges = draftTitle.isNotBlank() && draftTitle != persistedTitle
            val shouldNavigateBack = if (hasTitleChanges) {
                updateMetadataInternal(
                    id = entryId,
                    title = draftTitle,
                    moodScore = null,
                    expectedVersion = currentVersion
                )
            } else {
                forceSaveInternal(entryId)
            }

            if (shouldNavigateBack) {
                _uiState.update { it.copy(pendingNavigateBack = true) }
            }
        }
    }

    fun deleteEntry(entryId: String) {
        launchSafely(
            operation = "deleteEntry",
            onError = {
                _uiState.update { state ->
                    state.copy(isLoading = false, errorMessage = "Unable to delete this entry right now.")
                }
            }
        ) {
            _uiState.update { it.copy(isLoading = true) }
            when (val result = journalStore.deleteEntry(entryId)) {
                is JournalResult.Success -> {
                    _uiState.update { it.copy(isLoading = false, entry = null, pendingNavigateBack = true) }
                }

                is JournalResult.Error -> {
                    _uiState.update { it.copy(isLoading = false, errorMessage = result.error.toUserMessage()) }
                }

                is JournalResult.Loading -> Unit
            }
        }
    }

    fun consumeNavigateBack() {
        _uiState.update { it.copy(pendingNavigateBack = false) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private suspend fun updateMetadataInternal(
        id: String,
        title: String?,
        moodScore: Int?,
        expectedVersion: Long?
    ): Boolean {
        return when (val result = journalStore.updateMetadata(id, title, moodScore, expectedVersion)) {
            is JournalResult.Success -> {
                currentVersion = result.data.version
                true
            }

            is JournalResult.Error -> {
                val message = result.error.toUserMessage()
                _uiState.update { it.copy(errorMessage = message) }
                false
            }

            is JournalResult.Loading -> false
        }
    }

    private suspend fun forceSaveInternal(entryId: String): Boolean {
        autoSaveJob?.cancel()
        val body = pendingBodyUpdate ?: _uiState.value.entry?.body
        if (body == null) {
            return true
        }

        _uiState.update { it.copy(isSaving = true) }
        return when (val result = journalStore.updateBody(entryId, body, currentVersion)) {
            is JournalResult.Success -> {
                currentVersion = result.data.version
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        lastSavedAtEpochMillis = result.data.updatedAt.toEpochMilliseconds()
                    )
                }
                pendingBodyUpdate = null
                true
            }

            is JournalResult.Error -> {
                _uiState.update { it.copy(isSaving = false, errorMessage = result.error.toUserMessage()) }
                false
            }

            is JournalResult.Loading -> false
        }
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
