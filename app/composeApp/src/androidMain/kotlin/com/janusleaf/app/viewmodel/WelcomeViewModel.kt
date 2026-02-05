package com.janusleaf.app.viewmodel

import androidx.lifecycle.ViewModel
import com.janusleaf.app.viewmodel.state.AuthUiState
import com.janusleaf.app.model.store.AuthStore
import kotlinx.coroutines.flow.StateFlow

class WelcomeViewModel(
    private val authStore: AuthStore
) : ViewModel() {

    val authState: StateFlow<AuthUiState> = authStore.uiState

    fun logout() {
        authStore.logout()
    }
}
