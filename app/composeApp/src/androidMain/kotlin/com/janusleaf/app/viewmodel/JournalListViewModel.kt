package com.janusleaf.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.janusleaf.app.domain.model.InspirationError
import com.janusleaf.app.domain.model.InspirationResult
import com.janusleaf.app.domain.model.Journal
import com.janusleaf.app.domain.model.JournalResult
import com.janusleaf.app.viewmodel.state.AuthUiState
import com.janusleaf.app.viewmodel.state.InspirationUiState
import com.janusleaf.app.viewmodel.state.JournalListUiState
import com.janusleaf.app.model.data.store.AuthStore
import com.janusleaf.app.model.data.store.InspirationStore
import com.janusleaf.app.model.data.store.JournalStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val PAGE_SIZE = 20

class JournalListViewModel(
    private val authStore: AuthStore,
    private val journalStore: JournalStore,
    private val inspirationStore: InspirationStore
) : ViewModel() {

    val authState: StateFlow<AuthUiState> = authStore.uiState

    private val _uiState = MutableStateFlow(JournalListUiState())
    val uiState: StateFlow<JournalListUiState> = _uiState.asStateFlow()

    private val _inspirationState = MutableStateFlow(InspirationUiState())
    val inspirationState: StateFlow<InspirationUiState> = _inspirationState.asStateFlow()

    private var currentPage = 0
    private var isLoadingMore = false

    init {
        journalStore.observeEntries()
            .onEach { entries -> _uiState.update { it.copy(entries = entries) } }
            .launchIn(viewModelScope)

        inspirationStore.observeQuote()
            .onEach { quote -> _inspirationState.update { it.copy(quote = quote) } }
            .launchIn(viewModelScope)
    }

    fun loadEntries() {
        if (_uiState.value.isLoading) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            currentPage = 0
            when (val result = journalStore.loadEntries(page = 0, size = PAGE_SIZE)) {
                is JournalResult.Success -> {
                    currentPage = result.data.page
                    _uiState.update { it.copy(isLoading = false, hasMore = result.data.hasNext) }
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
            when (val result = journalStore.loadEntries(page = currentPage + 1, size = PAGE_SIZE)) {
                is JournalResult.Success -> {
                    currentPage = result.data.page
                    _uiState.update { it.copy(hasMore = result.data.hasNext) }
                }
                is JournalResult.Error -> {
                    _uiState.update { it.copy(errorMessage = result.error.toUserMessage()) }
                }
                is JournalResult.Loading -> Unit
            }
            isLoadingMore = false
        }
    }

    fun createEntry(onComplete: (Journal?) -> Unit = {}) {
        if (_uiState.value.isCreatingEntry) return
        viewModelScope.launch {
            _uiState.update { it.copy(isCreatingEntry = true, errorMessage = null) }
            when (val result = journalStore.createEntry()) {
                is JournalResult.Success -> {
                    _uiState.update { it.copy(isCreatingEntry = false) }
                    onComplete(result.data)
                }
                is JournalResult.Error -> {
                    _uiState.update {
                        it.copy(isCreatingEntry = false, errorMessage = result.error.toUserMessage())
                    }
                    onComplete(null)
                }
                is JournalResult.Loading -> Unit
            }
        }
    }

    fun fetchQuote() {
        if (_inspirationState.value.isLoading) return
        viewModelScope.launch {
            _inspirationState.update { it.copy(isLoading = true, errorMessage = null, isNotFound = false) }
            when (val result = inspirationStore.fetchQuote()) {
                is InspirationResult.Success -> {
                    _inspirationState.update {
                        it.copy(isLoading = false, quote = result.data, isNotFound = false)
                    }
                }
                is InspirationResult.Error -> {
                    if (result.error is InspirationError.NotFound) {
                        _inspirationState.update {
                            it.copy(isLoading = false, errorMessage = null, isNotFound = true, quote = null)
                        }
                    } else {
                        _inspirationState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = result.error.toUserMessage(),
                                isNotFound = false
                            )
                        }
                    }
                }
                is InspirationResult.Loading -> Unit
            }
        }
    }
}
