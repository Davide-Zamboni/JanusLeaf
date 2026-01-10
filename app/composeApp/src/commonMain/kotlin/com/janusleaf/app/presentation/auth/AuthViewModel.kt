package com.janusleaf.app.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.janusleaf.app.domain.model.AuthResult
import com.janusleaf.app.domain.repository.AuthRepository
import io.github.aakira.napier.Napier
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for authentication screens using MVI pattern.
 */
class AuthViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {
    
    private val _state = MutableStateFlow(AuthState())
    val state: StateFlow<AuthState> = _state.asStateFlow()
    
    private val _sideEffects = Channel<AuthSideEffect>(Channel.BUFFERED)
    val sideEffects = _sideEffects.receiveAsFlow()
    
    init {
        checkAuthState()
    }
    
    /**
     * Handle incoming events from the UI.
     */
    fun onEvent(event: AuthEvent) {
        when (event) {
            is AuthEvent.EmailChanged -> updateEmail(event.email)
            is AuthEvent.PasswordChanged -> updatePassword(event.password)
            is AuthEvent.UsernameChanged -> updateUsername(event.username)
            is AuthEvent.ConfirmPasswordChanged -> updateConfirmPassword(event.confirmPassword)
            is AuthEvent.TogglePasswordVisibility -> togglePasswordVisibility()
            is AuthEvent.ToggleConfirmPasswordVisibility -> toggleConfirmPasswordVisibility()
            is AuthEvent.ToggleAuthMode -> toggleAuthMode()
            is AuthEvent.Submit -> submit()
            is AuthEvent.ClearError -> clearError()
            is AuthEvent.CheckAuthState -> checkAuthState()
        }
    }
    
    private fun updateEmail(email: String) {
        _state.update { state ->
            state.copy(
                email = email,
                emailError = validateEmail(email),
                error = null
            )
        }
    }
    
    private fun updatePassword(password: String) {
        _state.update { state ->
            state.copy(
                password = password,
                passwordError = validatePassword(password),
                confirmPasswordError = if (state.authMode == AuthMode.REGISTER && 
                    state.confirmPassword.isNotEmpty()) {
                    validateConfirmPassword(state.confirmPassword, password)
                } else null,
                error = null
            )
        }
    }
    
    private fun updateUsername(username: String) {
        _state.update { state ->
            state.copy(
                username = username,
                usernameError = validateUsername(username),
                error = null
            )
        }
    }
    
    private fun updateConfirmPassword(confirmPassword: String) {
        _state.update { state ->
            state.copy(
                confirmPassword = confirmPassword,
                confirmPasswordError = validateConfirmPassword(confirmPassword, state.password),
                error = null
            )
        }
    }
    
    private fun togglePasswordVisibility() {
        _state.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }
    
    private fun toggleConfirmPasswordVisibility() {
        _state.update { it.copy(isConfirmPasswordVisible = !it.isConfirmPasswordVisible) }
    }
    
    private fun toggleAuthMode() {
        _state.update { state ->
            state.copy(
                authMode = if (state.authMode == AuthMode.LOGIN) AuthMode.REGISTER else AuthMode.LOGIN,
                error = null,
                emailError = null,
                passwordError = null,
                usernameError = null,
                confirmPasswordError = null,
                password = "",
                confirmPassword = ""
            )
        }
    }
    
    private fun clearError() {
        _state.update { it.copy(error = null) }
    }
    
    private fun checkAuthState() {
        viewModelScope.launch {
            val isAuthenticated = authRepository.isAuthenticated()
            if (isAuthenticated) {
                _state.update { it.copy(isAuthenticated = true) }
                _sideEffects.send(AuthSideEffect.NavigateToHome)
            }
        }
    }
    
    private fun submit() {
        val currentState = _state.value
        
        if (!currentState.isFormValid) {
            Napier.w("Form validation failed", tag = "AuthViewModel")
            return
        }
        
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            
            val result = when (currentState.authMode) {
                AuthMode.LOGIN -> authRepository.login(
                    email = currentState.email.trim(),
                    password = currentState.password
                )
                AuthMode.REGISTER -> authRepository.register(
                    email = currentState.email.trim(),
                    username = currentState.username.trim(),
                    password = currentState.password
                )
            }
            
            when (result) {
                is AuthResult.Success -> {
                    Napier.d("Auth successful", tag = "AuthViewModel")
                    _state.update { state ->
                        state.copy(
                            isLoading = false,
                            isAuthenticated = true,
                            currentUser = result.data.user,
                            error = null
                        )
                    }
                    _sideEffects.send(AuthSideEffect.NavigateToHome)
                    _sideEffects.send(AuthSideEffect.ShowToast(
                        if (currentState.authMode == AuthMode.LOGIN) "Welcome back!" 
                        else "Account created successfully!"
                    ))
                }
                is AuthResult.Error -> {
                    Napier.e("Auth failed: ${result.error}", tag = "AuthViewModel")
                    _state.update { state ->
                        state.copy(
                            isLoading = false,
                            error = result.error.toUserMessage()
                        )
                    }
                }
                is AuthResult.Loading -> {
                    // Already handled
                }
            }
        }
    }
    
    // ==================== Validation ====================
    
    private fun validateEmail(email: String): String? {
        if (email.isBlank()) return null // Don't show error for empty field
        val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$".toRegex()
        return if (!email.matches(emailRegex)) "Enter a valid email address" else null
    }
    
    private fun validatePassword(password: String): String? {
        if (password.isBlank()) return null
        return if (password.length < 8) "Password must be at least 8 characters" else null
    }
    
    private fun validateUsername(username: String): String? {
        if (username.isBlank()) return null
        return when {
            username.length < 2 -> "Username must be at least 2 characters"
            username.length > 50 -> "Username must be less than 50 characters"
            else -> null
        }
    }
    
    private fun validateConfirmPassword(confirmPassword: String, password: String): String? {
        if (confirmPassword.isBlank()) return null
        return if (confirmPassword != password) "Passwords don't match" else null
    }
}
