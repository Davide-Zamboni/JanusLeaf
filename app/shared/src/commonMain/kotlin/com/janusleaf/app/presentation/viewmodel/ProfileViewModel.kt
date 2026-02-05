package com.janusleaf.app.presentation.viewmodel

import com.janusleaf.app.domain.model.JournalPreview
import com.janusleaf.app.model.store.AuthStore
import com.janusleaf.app.model.store.JournalStore
import com.janusleaf.app.model.store.state.AuthUiState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class ProfileViewModel(
    private val authStore: AuthStore,
    private val journalStore: JournalStore
) : KmpViewModel() {

    val authState: StateFlow<AuthUiState> = authStore.uiState

    val entries: StateFlow<List<JournalPreview>> = journalStore.observeEntries()
        .stateIn(scope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun logout() {
        authStore.logout()
    }
}
