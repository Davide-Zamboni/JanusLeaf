package com.janusleaf.app.presentation.viewmodel

import com.janusleaf.app.model.store.AuthStore
import com.janusleaf.app.model.store.state.AuthUiState
import kotlinx.coroutines.flow.StateFlow

class AuthFormViewModel(
    private val authStore: AuthStore
) : KmpViewModel() {

    val uiState: StateFlow<AuthUiState> = authStore.uiState

    fun login(email: String, password: String) {
        authStore.login(email, password)
    }

    fun register(email: String, username: String, password: String) {
        authStore.register(email, username, password)
    }

    fun clearError() {
        authStore.clearError()
    }

    fun isValidEmail(email: String): Boolean = authStore.isValidEmail(email)

    fun isValidPassword(password: String): Boolean = authStore.isValidPassword(password)

    fun isValidUsername(username: String): Boolean = authStore.isValidUsername(username)
}
