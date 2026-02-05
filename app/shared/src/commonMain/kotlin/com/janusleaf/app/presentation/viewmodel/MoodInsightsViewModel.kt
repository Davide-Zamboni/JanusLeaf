package com.janusleaf.app.presentation.viewmodel

import com.janusleaf.app.domain.model.JournalResult
import com.janusleaf.app.model.store.JournalStore
import com.janusleaf.app.presentation.state.MoodInsightsUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update

private const val PAGE_SIZE = 20

class MoodInsightsViewModel(
    private val journalStore: JournalStore
) : KmpViewModel() {

    private val _uiState = MutableStateFlow(MoodInsightsUiState())
    val uiState: StateFlow<MoodInsightsUiState> = _uiState.asStateFlow()

    init {
        journalStore.observeEntries()
            .onEach { entries -> _uiState.update { it.copy(entries = entries) } }
            .launchIn(scope)
    }

    fun loadEntries() {
        if (_uiState.value.isLoading) return
        launchSafely(
            operation = "loadMoodEntries",
            onError = {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = "Unable to load entries right now.")
                }
            }
        ) {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = journalStore.loadEntries(page = 0, size = PAGE_SIZE)) {
                is JournalResult.Success -> _uiState.update { it.copy(isLoading = false) }
                is JournalResult.Error -> {
                    _uiState.update { it.copy(isLoading = false, errorMessage = result.error.toUserMessage()) }
                }

                is JournalResult.Loading -> Unit
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
