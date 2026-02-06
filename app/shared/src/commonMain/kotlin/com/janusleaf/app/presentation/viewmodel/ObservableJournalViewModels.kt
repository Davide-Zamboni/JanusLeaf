package com.janusleaf.app.presentation.viewmodel

import com.janusleaf.app.domain.model.InspirationalQuote
import com.janusleaf.app.domain.model.Journal
import com.janusleaf.app.domain.model.JournalPreview
import com.janusleaf.app.model.store.state.AuthUiState
import com.janusleaf.app.presentation.state.InspirationUiState
import com.janusleaf.app.presentation.state.JournalEditorUiState
import com.janusleaf.app.presentation.state.JournalListUiState
import com.janusleaf.app.presentation.state.MoodInsightsUiState
import com.rickclephas.kmp.observableviewmodel.MutableStateFlow
import com.rickclephas.kmp.observableviewmodel.ViewModel
import com.rickclephas.kmp.observableviewmodel.coroutineScope
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

class ObservableJournalListViewModel(
    private val viewModel: JournalListViewModel
) : ViewModel() {

    private val _authState = MutableStateFlow(viewModelScope, viewModel.authState.value)
    val authState: StateFlow<AuthUiState> = _authState.asStateFlow()

    private val _uiState = MutableStateFlow(viewModelScope, viewModel.uiState.value)
    val uiState: StateFlow<JournalListUiState> = _uiState.asStateFlow()

    private val _inspirationState = MutableStateFlow(viewModelScope, viewModel.inspirationState.value)
    val inspirationState: StateFlow<InspirationUiState> = _inspirationState.asStateFlow()

    val isLoading: Boolean get() = uiState.value.isLoading
    val errorMessage: String? get() = uiState.value.errorMessage
    val entries: List<JournalPreview> get() = uiState.value.entries
    val hasMore: Boolean get() = uiState.value.hasMore
    val isCreatingEntry: Boolean get() = uiState.value.isCreatingEntry
    val isAuthenticated: Boolean get() = authState.value.isAuthenticated
    val currentUserEmail: String? get() = authState.value.user?.email
    val currentUsername: String? get() = authState.value.user?.username

    val inspirationIsLoading: Boolean get() = inspirationState.value.isLoading
    val inspirationIsNotFound: Boolean get() = inspirationState.value.isNotFound
    val inspirationQuote: InspirationalQuote? get() = inspirationState.value.quote
    val inspirationErrorMessage: String? get() = inspirationState.value.errorMessage

    init {
        observeAuthState()
        observeUiState()
        observeInspirationState()
    }

    fun loadEntries() {
        viewModel.loadEntries()
    }

    fun loadMoreEntries() {
        viewModel.loadMoreEntries()
    }

    fun refresh() {
        viewModel.loadEntries()
    }

    fun fetchQuote() {
        viewModel.fetchQuote()
    }

    fun refreshQuote() {
        viewModel.fetchQuote()
    }

    fun createEntry(
        title: String? = null,
        body: String? = null,
        entryDate: LocalDate? = null,
        onComplete: (Journal?) -> Unit = {}
    ) {
        viewModel.createEntry(title, body, entryDate, onComplete)
    }

    fun clearError() {
        viewModel.clearError()
        viewModel.clearInspirationError()
    }

    fun logout() {
        viewModel.logout()
    }

    override fun onCleared() {
        super.onCleared()
        viewModel.clear()
    }

    private fun observeUiState() {
        viewModelScope.coroutineScope.launch {
            try {
                viewModel.uiState.collect { state ->
                    _uiState.value = state
                }
            } catch (cancellationException: CancellationException) {
                throw cancellationException
            } catch (throwable: Throwable) {
                Napier.e("Failed to observe journal list state", throwable, tag = "ObservableJournalListViewModel")
            }
        }
    }

    private fun observeAuthState() {
        viewModelScope.coroutineScope.launch {
            try {
                viewModel.authState.collect { state ->
                    _authState.value = state
                }
            } catch (cancellationException: CancellationException) {
                throw cancellationException
            } catch (throwable: Throwable) {
                Napier.e("Failed to observe auth state", throwable, tag = "ObservableJournalListViewModel")
            }
        }
    }

    private fun observeInspirationState() {
        viewModelScope.coroutineScope.launch {
            try {
                viewModel.inspirationState.collect { state ->
                    _inspirationState.value = state
                }
            } catch (cancellationException: CancellationException) {
                throw cancellationException
            } catch (throwable: Throwable) {
                Napier.e("Failed to observe inspiration state", throwable, tag = "ObservableJournalListViewModel")
            }
        }
    }
}

class ObservableJournalEditorViewModel(
    private val viewModel: JournalEditorViewModel
) : ViewModel() {

    private val _uiState = MutableStateFlow(viewModelScope, viewModel.uiState.value)
    val uiState: StateFlow<JournalEditorUiState> = _uiState.asStateFlow()

    val isLoading: Boolean get() = uiState.value.isLoading
    val errorMessage: String? get() = uiState.value.errorMessage
    val currentEntry: Journal? get() = uiState.value.entry
    val isSaving: Boolean get() = uiState.value.isSaving
    val lastSavedAtEpochMillis: Long? get() = uiState.value.lastSavedAtEpochMillis

    init {
        viewModelScope.coroutineScope.launch {
            try {
                viewModel.uiState.collect { state ->
                    _uiState.value = state
                }
            } catch (cancellationException: CancellationException) {
                throw cancellationException
            } catch (throwable: Throwable) {
                Napier.e("Failed to observe journal editor state", throwable, tag = "ObservableJournalEditorViewModel")
            }
        }
    }

    fun loadEntry(id: String, onComplete: (Journal?) -> Unit) {
        viewModel.bindEntry(entryId = id)
        viewModel.loadEntry(
            entryId = id,
            onSuccess = { journal -> onComplete(journal) },
            onError = { onComplete(null) }
        )
    }

    fun updateBody(body: String, entryId: String) {
        viewModel.updateBody(entryId = entryId, body = body)
    }

    fun updateTitle(title: String, entryId: String, onComplete: (Boolean) -> Unit = {}) {
        viewModel.updateTitle(entryId = entryId, title = title, onComplete = onComplete)
    }

    fun updateMoodScore(score: Int, entryId: String, onComplete: (Boolean) -> Unit = {}) {
        viewModel.updateMoodScore(entryId = entryId, score = score, onComplete = onComplete)
    }

    fun forceSave(entryId: String, onComplete: (Boolean) -> Unit = {}) {
        viewModel.forceSave(entryId = entryId, onComplete = onComplete)
    }

    fun deleteEntry(entryId: String, onComplete: (Boolean) -> Unit = {}) {
        viewModel.deleteEntry(entryId = entryId, onComplete = onComplete)
    }

    fun clearCurrentEntry() {
        viewModel.clearBoundEntry()
    }

    fun clearError() {
        viewModel.clearError()
    }

    override fun onCleared() {
        super.onCleared()
        viewModel.clear()
    }
}

class ObservableMoodInsightsViewModel(
    private val viewModel: MoodInsightsViewModel
) : ViewModel() {

    private val _authState = MutableStateFlow(viewModelScope, viewModel.authState.value)
    val authState: StateFlow<AuthUiState> = _authState.asStateFlow()

    private val _uiState = MutableStateFlow(viewModelScope, viewModel.uiState.value)
    val uiState: StateFlow<MoodInsightsUiState> = _uiState.asStateFlow()

    val isLoading: Boolean get() = uiState.value.isLoading
    val errorMessage: String? get() = uiState.value.errorMessage
    val entries: List<JournalPreview> get() = uiState.value.entries
    val isAuthenticated: Boolean get() = authState.value.isAuthenticated
    val currentUserEmail: String? get() = authState.value.user?.email
    val currentUsername: String? get() = authState.value.user?.username

    init {
        observeAuthState()
        viewModelScope.coroutineScope.launch {
            try {
                viewModel.uiState.collect { state ->
                    _uiState.value = state
                }
            } catch (cancellationException: CancellationException) {
                throw cancellationException
            } catch (throwable: Throwable) {
                Napier.e("Failed to observe mood insights state", throwable, tag = "ObservableMoodInsightsViewModel")
            }
        }
    }

    fun loadEntries() {
        viewModel.loadEntries()
    }

    fun clearError() {
        viewModel.clearError()
    }

    fun logout() {
        viewModel.logout()
    }

    override fun onCleared() {
        super.onCleared()
        viewModel.clear()
    }

    private fun observeAuthState() {
        viewModelScope.coroutineScope.launch {
            try {
                viewModel.authState.collect { state ->
                    _authState.value = state
                }
            } catch (cancellationException: CancellationException) {
                throw cancellationException
            } catch (throwable: Throwable) {
                Napier.e("Failed to observe mood auth state", throwable, tag = "ObservableMoodInsightsViewModel")
            }
        }
    }
}

class ObservableProfileViewModel(
    private val viewModel: ProfileViewModel
) : ViewModel() {

    private val _authState = MutableStateFlow(viewModelScope, viewModel.authState.value)
    val authState: StateFlow<AuthUiState> = _authState.asStateFlow()

    private val _entries = MutableStateFlow(viewModelScope, viewModel.entries.value)
    val entriesState: StateFlow<List<JournalPreview>> = _entries.asStateFlow()

    val currentUserEmail: String? get() = authState.value.user?.email
    val currentUsername: String? get() = authState.value.user?.username
    val isAuthenticated: Boolean get() = authState.value.isAuthenticated
    val entries: List<JournalPreview> get() = entriesState.value

    init {
        observeAuthState()
        observeEntries()
    }

    fun logout() {
        viewModel.logout()
    }

    override fun onCleared() {
        super.onCleared()
        viewModel.clear()
    }

    private fun observeAuthState() {
        viewModelScope.coroutineScope.launch {
            try {
                viewModel.authState.collect { state ->
                    _authState.value = state
                }
            } catch (cancellationException: CancellationException) {
                throw cancellationException
            } catch (throwable: Throwable) {
                Napier.e("Failed to observe profile auth state", throwable, tag = "ObservableProfileViewModel")
            }
        }
    }

    private fun observeEntries() {
        viewModelScope.coroutineScope.launch {
            try {
                viewModel.entries.collect { entries ->
                    _entries.value = entries
                }
            } catch (cancellationException: CancellationException) {
                throw cancellationException
            } catch (throwable: Throwable) {
                Napier.e("Failed to observe profile entries", throwable, tag = "ObservableProfileViewModel")
            }
        }
    }
}
