package com.janusleaf.app.presentation.viewmodel

import com.janusleaf.app.model.store.AuthStore
import com.janusleaf.app.model.store.state.AuthUiState
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.dropWhile
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

class AuthViewModel(
    private val authStore: AuthStore
) : KmpViewModel() {

    val uiState: StateFlow<AuthUiState> = authStore.uiState

    fun login(email: String, password: String) {
        authStore.login(email, password)
    }

    fun login(
        email: String,
        password: String,
        onComplete: (success: Boolean, error: String?) -> Unit
    ) {
        launchSafely(operation = "login") {
            val finalState = awaitAuthAction { authStore.login(email, password) }
            onComplete(
                finalState?.isAuthenticated == true && finalState.errorMessage == null,
                when {
                    finalState == null -> "Authentication timed out. Please try again."
                    finalState.isAuthenticated && finalState.errorMessage == null -> null
                    else -> finalState.errorMessage ?: "Authentication failed"
                }
            )
        }
    }

    fun register(email: String, username: String, password: String) {
        authStore.register(email, username, password)
    }

    fun register(
        email: String,
        username: String,
        password: String,
        onComplete: (success: Boolean, error: String?) -> Unit
    ) {
        launchSafely(operation = "register") {
            val finalState = awaitAuthAction { authStore.register(email, username, password) }
            onComplete(
                finalState?.isAuthenticated == true && finalState.errorMessage == null,
                when {
                    finalState == null -> "Registration timed out. Please try again."
                    finalState.isAuthenticated && finalState.errorMessage == null -> null
                    else -> finalState.errorMessage ?: "Registration failed"
                }
            )
        }
    }

    fun logout() {
        authStore.logout()
    }

    fun logout(onComplete: () -> Unit) {
        launchSafely(operation = "logout") {
            authStore.logout()
            onComplete()
        }
    }

    fun clearError() {
        authStore.clearError()
    }

    fun isValidEmail(email: String): Boolean = authStore.isValidEmail(email)

    fun isValidPassword(password: String): Boolean = authStore.isValidPassword(password)

    fun isValidUsername(username: String): Boolean = authStore.isValidUsername(username)

    private suspend fun awaitAuthAction(
        action: () -> Unit
    ): AuthUiState? = withTimeoutOrNull(15_000) {
        action()
        authStore.uiState
            .dropWhile { !it.isLoading }
            .dropWhile { it.isLoading }
            .first()
    }
}
