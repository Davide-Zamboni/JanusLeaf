package com.janusleaf.app.presentation.viewmodel

import com.janusleaf.app.model.store.state.AuthUiState
import com.rickclephas.kmp.observableviewmodel.MutableStateFlow
import com.rickclephas.kmp.observableviewmodel.ViewModel
import com.rickclephas.kmp.observableviewmodel.coroutineScope
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class ObservableAuthFormViewModel(
    private val viewModel: AuthFormViewModel
) : ViewModel() {

    private val _uiState = MutableStateFlow(viewModelScope, viewModel.uiState.value)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    val isLoading: Boolean get() = uiState.value.isLoading
    val isAuthenticated: Boolean get() = uiState.value.isAuthenticated
    val errorMessage: String? get() = uiState.value.errorMessage

    init {
        viewModelScope.coroutineScope.launch {
            try {
                viewModel.uiState.collect { state ->
                    _uiState.value = state
                }
            } catch (cancellationException: CancellationException) {
                throw cancellationException
            } catch (throwable: Throwable) {
                Napier.e("Failed to observe auth form state", throwable, tag = "ObservableAuthFormViewModel")
            }
        }
    }

    fun login(email: String, password: String) {
        viewModel.login(email, password)
    }

    fun register(email: String, username: String, password: String) {
        viewModel.register(email, username, password)
    }

    fun clearError() {
        viewModel.clearError()
    }

    fun isValidEmail(email: String): Boolean = viewModel.isValidEmail(email)

    fun isValidPassword(password: String): Boolean = viewModel.isValidPassword(password)

    fun isValidUsername(username: String): Boolean = viewModel.isValidUsername(username)

    override fun onCleared() {
        super.onCleared()
        viewModel.clear()
    }
}

class ObservableSessionViewModel(
    private val viewModel: WelcomeViewModel
) : ViewModel() {

    private val _authState = MutableStateFlow(viewModelScope, viewModel.authState.value)
    val authState: StateFlow<AuthUiState> = _authState.asStateFlow()

    val isLoading: Boolean get() = authState.value.isLoading
    val isAuthenticated: Boolean get() = authState.value.isAuthenticated
    val errorMessage: String? get() = authState.value.errorMessage
    val currentUserEmail: String? get() = authState.value.user?.email
    val currentUsername: String? get() = authState.value.user?.username

    init {
        viewModelScope.coroutineScope.launch {
            try {
                viewModel.authState.collect { state ->
                    _authState.value = state
                }
            } catch (cancellationException: CancellationException) {
                throw cancellationException
            } catch (throwable: Throwable) {
                Napier.e("Failed to observe session state", throwable, tag = "ObservableSessionViewModel")
            }
        }
    }

    fun logout() {
        viewModel.logout()
    }

    override fun onCleared() {
        super.onCleared()
        viewModel.clear()
    }
}
