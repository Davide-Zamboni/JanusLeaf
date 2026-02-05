package com.janusleaf.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.janusleaf.app.domain.model.AuthResult
import com.janusleaf.app.domain.model.User
import com.janusleaf.app.domain.repository.AuthRepository
import com.janusleaf.app.domain.repository.TokenStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthUiState(
    val isLoading: Boolean = false,
    val isAuthenticated: Boolean = false,
    val errorMessage: String? = null,
    val user: User? = null
)

class AuthViewModel(
    private val authRepository: AuthRepository,
    private val tokenStorage: TokenStorage
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val isAuth = authRepository.isAuthenticated()
            _uiState.update { it.copy(isAuthenticated = isAuth) }
            if (isAuth) {
                refreshUser()
            }
        }
        viewModelScope.launch {
            authRepository.observeAuthState().collect { isAuth ->
                _uiState.update { it.copy(isAuthenticated = isAuth) }
                if (isAuth) {
                    refreshUser()
                } else {
                    _uiState.update { it.copy(user = null) }
                }
            }
        }
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = authRepository.login(email, password)) {
                is AuthResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isAuthenticated = true,
                            user = result.data.user
                        )
                    }
                }
                is AuthResult.Error -> {
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = result.error.toUserMessage())
                    }
                }
                is AuthResult.Loading -> Unit
            }
        }
    }

    fun register(email: String, username: String, password: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = authRepository.register(email, username, password)) {
                is AuthResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isAuthenticated = true,
                            user = result.data.user
                        )
                    }
                }
                is AuthResult.Error -> {
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = result.error.toUserMessage())
                    }
                }
                is AuthResult.Loading -> Unit
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val refreshToken = tokenStorage.getRefreshToken()
            if (refreshToken != null) {
                authRepository.logout(refreshToken)
            }
            authRepository.clearAuthData()
            _uiState.update { it.copy(isLoading = false, isAuthenticated = false, user = null) }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun isValidEmail(email: String): Boolean {
        if (email.isBlank()) return false
        val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$".toRegex()
        return email.matches(emailRegex)
    }

    fun isValidPassword(password: String): Boolean = password.length >= 8

    fun isValidUsername(username: String): Boolean = username.length in 2..50

    private fun refreshUser() {
        viewModelScope.launch {
            when (val result = authRepository.getCurrentUser()) {
                is AuthResult.Success -> {
                    _uiState.update { it.copy(user = result.data) }
                }
                is AuthResult.Error -> {
                    _uiState.update { it.copy(errorMessage = result.error.toUserMessage()) }
                }
                is AuthResult.Loading -> Unit
            }
        }
    }
}
