package com.janusleaf.app.presentation.viewmodel

import com.janusleaf.app.model.store.AuthStore
import com.janusleaf.app.model.store.state.AuthUiState
import kotlinx.coroutines.flow.StateFlow

class WelcomeViewModel(
    private val authStore: AuthStore
) : KmpViewModel() {

    val authState: StateFlow<AuthUiState> = authStore.uiState

    fun logout() {
        authStore.logout()
    }
}
