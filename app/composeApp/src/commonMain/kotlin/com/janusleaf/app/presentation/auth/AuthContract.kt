package com.janusleaf.app.presentation.auth

import com.janusleaf.app.domain.model.User

/**
 * MVI Contract for Authentication screens.
 * Defines the State, Events, and Side Effects.
 */

/**
 * Represents the UI state for authentication screens.
 */
data class AuthState(
    val email: String = "",
    val password: String = "",
    val username: String = "",
    val confirmPassword: String = "",
    val isLoading: Boolean = false,
    val isPasswordVisible: Boolean = false,
    val isConfirmPasswordVisible: Boolean = false,
    val error: String? = null,
    val isAuthenticated: Boolean = false,
    val currentUser: User? = null,
    val authMode: AuthMode = AuthMode.LOGIN,
    val emailError: String? = null,
    val passwordError: String? = null,
    val usernameError: String? = null,
    val confirmPasswordError: String? = null
) {
    val isFormValid: Boolean
        get() = when (authMode) {
            AuthMode.LOGIN -> email.isNotBlank() && password.isNotBlank() &&
                              emailError == null && passwordError == null
            AuthMode.REGISTER -> email.isNotBlank() && password.isNotBlank() &&
                                 username.isNotBlank() && confirmPassword.isNotBlank() &&
                                 emailError == null && passwordError == null &&
                                 usernameError == null && confirmPasswordError == null &&
                                 password == confirmPassword
        }
}

/**
 * Authentication mode (Login or Register).
 */
enum class AuthMode {
    LOGIN,
    REGISTER
}

/**
 * Events that can be triggered by the user.
 */
sealed class AuthEvent {
    data class EmailChanged(val email: String) : AuthEvent()
    data class PasswordChanged(val password: String) : AuthEvent()
    data class UsernameChanged(val username: String) : AuthEvent()
    data class ConfirmPasswordChanged(val confirmPassword: String) : AuthEvent()
    data object TogglePasswordVisibility : AuthEvent()
    data object ToggleConfirmPasswordVisibility : AuthEvent()
    data object ToggleAuthMode : AuthEvent()
    data object Submit : AuthEvent()
    data object ClearError : AuthEvent()
    data object CheckAuthState : AuthEvent()
}

/**
 * One-time side effects that should trigger navigation or show toasts.
 */
sealed class AuthSideEffect {
    data object NavigateToHome : AuthSideEffect()
    data class ShowToast(val message: String) : AuthSideEffect()
}
