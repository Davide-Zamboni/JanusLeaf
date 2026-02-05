package com.janusleaf.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.janusleaf.app.data.remote.AuthApiService
import com.janusleaf.app.data.remote.InspirationApiService
import com.janusleaf.app.domain.model.AuthResult
import com.janusleaf.app.domain.model.InspirationError
import com.janusleaf.app.domain.model.InspirationResult
import com.janusleaf.app.domain.model.InspirationalQuote
import com.janusleaf.app.domain.repository.TokenStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

 data class InspirationUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val quote: InspirationalQuote? = null,
    val isNotFound: Boolean = false
)

class InspirationViewModel(
    private val inspirationApiService: InspirationApiService,
    private val authApiService: AuthApiService,
    private val tokenStorage: TokenStorage
) : ViewModel() {

    private val _uiState = MutableStateFlow(InspirationUiState())
    val uiState: StateFlow<InspirationUiState> = _uiState.asStateFlow()

    fun fetchQuote() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, isNotFound = false) }
            val accessToken = tokenStorage.getAccessToken()
            if (accessToken == null) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = "Please log in to see your inspiration")
                }
                return@launch
            }
            when (val result = inspirationApiService.getInspiration(accessToken)) {
                is InspirationResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            quote = result.data.toDomain(),
                            isNotFound = false
                        )
                    }
                }
                is InspirationResult.Error -> {
                    handleInspirationError(result.error)
                }
                is InspirationResult.Loading -> Unit
            }
        }
    }

    fun refresh() {
        fetchQuote()
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun reset() {
        _uiState.value = InspirationUiState()
    }

    private suspend fun handleInspirationError(error: InspirationError) {
        when (error) {
            is InspirationError.NotFound -> {
                _uiState.update { it.copy(isLoading = false, quote = null, isNotFound = true) }
            }
            is InspirationError.Unauthorized -> {
                if (tryRefreshToken()) {
                    val newToken = tokenStorage.getAccessToken()
                    if (newToken != null) {
                        when (val retry = inspirationApiService.getInspiration(newToken)) {
                            is InspirationResult.Success -> {
                                _uiState.update {
                                    it.copy(
                                        isLoading = false,
                                        quote = retry.data.toDomain(),
                                        isNotFound = false
                                    )
                                }
                            }
                            is InspirationResult.Error -> {
                                if (retry.error is InspirationError.NotFound) {
                                    _uiState.update {
                                        it.copy(isLoading = false, quote = null, isNotFound = true)
                                    }
                                } else {
                                    _uiState.update {
                                        it.copy(isLoading = false, errorMessage = retry.error.toUserMessage())
                                    }
                                }
                            }
                            is InspirationResult.Loading -> Unit
                        }
                    }
                } else {
                    _uiState.update { it.copy(isLoading = false, errorMessage = error.toUserMessage()) }
                }
            }
            else -> {
                _uiState.update { it.copy(isLoading = false, errorMessage = error.toUserMessage()) }
            }
        }
    }

    private suspend fun tryRefreshToken(): Boolean {
        val refreshToken = tokenStorage.getRefreshToken() ?: return false
        return when (val result = authApiService.refreshToken(refreshToken)) {
            is AuthResult.Success -> {
                tokenStorage.saveAccessToken(result.data.accessToken)
                true
            }
            else -> false
        }
    }
}
