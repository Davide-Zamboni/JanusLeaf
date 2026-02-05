package com.janusleaf.app.model.store.state

import com.janusleaf.app.domain.model.User

data class AuthUiState(
    val isLoading: Boolean = false,
    val isAuthenticated: Boolean = false,
    val errorMessage: String? = null,
    val user: User? = null
)
