package com.janusleaf.app.viewmodel

import androidx.lifecycle.ViewModel
import com.janusleaf.app.viewmodel.state.AuthUiState
import com.janusleaf.app.model.data.store.AuthStore
import kotlinx.coroutines.flow.StateFlow

class AuthScreenViewModel(
    private val authStore: AuthStore
) : ViewModel() {

    val uiState: StateFlow<AuthUiState> = authStore.uiState

    fun login(email: String, password: String) {
        authStore.login(email, password)
    }

    fun register(email: String, username: String, password: String) {
        authStore.register(email, username, password)
    }

    fun logout() {
        authStore.logout()
    }

    fun clearError() {
        authStore.clearError()
    }

    fun isValidEmail(email: String): Boolean = authStore.isValidEmail(email)

    fun isValidPassword(password: String): Boolean = authStore.isValidPassword(password)

    fun isValidUsername(username: String): Boolean = authStore.isValidUsername(username)
}
