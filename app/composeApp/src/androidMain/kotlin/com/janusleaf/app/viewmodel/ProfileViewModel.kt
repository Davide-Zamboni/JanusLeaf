package com.janusleaf.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.janusleaf.app.domain.model.JournalPreview
import com.janusleaf.app.viewmodel.state.AuthUiState
import com.janusleaf.app.model.data.store.AuthStore
import com.janusleaf.app.model.data.store.JournalStore
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class ProfileViewModel(
    private val authStore: AuthStore,
    private val journalStore: JournalStore
) : ViewModel() {

    val authState: StateFlow<AuthUiState> = authStore.uiState

    val entries: StateFlow<List<JournalPreview>> = journalStore.observeEntries()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun logout() {
        authStore.logout()
    }
}
