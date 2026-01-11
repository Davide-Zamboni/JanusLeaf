package com.janusleaf.app.ios

import com.janusleaf.app.data.local.IosSecureTokenStorage
import com.janusleaf.app.data.remote.AuthApiService
import com.janusleaf.app.data.remote.InspirationApiService
import com.janusleaf.app.data.remote.createApiHttpClient
import com.janusleaf.app.data.remote.getPlatformBaseUrl
import com.janusleaf.app.domain.model.*
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * iOS-friendly inspiration service.
 * This class provides a simple API for SwiftUI to interact with inspiration functionality.
 */
class IosInspirationService {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    private val baseUrl = getPlatformBaseUrl()
    private val tokenStorage = IosSecureTokenStorage()
    private val httpClient = createApiHttpClient()
    private val inspirationApiService = InspirationApiService(httpClient, baseUrl)
    private val authApiService = AuthApiService(httpClient, baseUrl)
    
    // Observable state for SwiftUI
    private val _isLoading = MutableStateFlow(false)
    val isLoadingFlow: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessageFlow: StateFlow<String?> = _errorMessage.asStateFlow()
    
    private val _quote = MutableStateFlow<InspirationalQuote?>(null)
    val quoteFlow: StateFlow<InspirationalQuote?> = _quote.asStateFlow()
    
    private val _isNotFound = MutableStateFlow(false)
    val isNotFoundFlow: StateFlow<Boolean> = _isNotFound.asStateFlow()
    
    // Current values (for synchronous access)
    val isLoading: Boolean get() = _isLoading.value
    val errorMessage: String? get() = _errorMessage.value
    val quote: InspirationalQuote? get() = _quote.value
    val isNotFound: Boolean get() = _isNotFound.value
    
    init {
        Napier.i("IosInspirationService initialized", tag = "InspirationService")
    }
    
    /**
     * Fetch the inspirational quote for the current user.
     */
    fun fetchQuote(onComplete: () -> Unit = {}) {
        scope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _isNotFound.value = false
            
            // Get access token
            val accessToken = tokenStorage.getAccessToken()
            if (accessToken == null) {
                _errorMessage.value = "Please log in to see your inspiration"
                _isLoading.value = false
                onComplete()
                return@launch
            }
            
            when (val result = inspirationApiService.getInspiration(accessToken)) {
                is InspirationResult.Success -> {
                    _quote.value = result.data.toDomain()
                    _isNotFound.value = false
                    Napier.d("Loaded inspirational quote", tag = "InspirationService")
                }
                is InspirationResult.Error -> {
                    when (result.error) {
                        is InspirationError.NotFound -> {
                            _isNotFound.value = true
                            _quote.value = null
                            Napier.d("No quote available yet", tag = "InspirationService")
                        }
                        is InspirationError.Unauthorized -> {
                            // Try to refresh token
                            if (tryRefreshToken()) {
                                // Retry the request
                                val newToken = tokenStorage.getAccessToken()
                                if (newToken != null) {
                                    when (val retryResult = inspirationApiService.getInspiration(newToken)) {
                                        is InspirationResult.Success -> {
                                            _quote.value = retryResult.data.toDomain()
                                            _isNotFound.value = false
                                        }
                                        is InspirationResult.Error -> {
                                            if (retryResult.error is InspirationError.NotFound) {
                                                _isNotFound.value = true
                                                _quote.value = null
                                            } else {
                                                _errorMessage.value = retryResult.error.toUserMessage()
                                            }
                                        }
                                        is InspirationResult.Loading -> {}
                                    }
                                }
                            } else {
                                _errorMessage.value = result.error.toUserMessage()
                            }
                        }
                        else -> {
                            _errorMessage.value = result.error.toUserMessage()
                            Napier.e("Failed to load quote: ${result.error}", tag = "InspirationService")
                        }
                    }
                }
                is InspirationResult.Loading -> {}
            }
            
            _isLoading.value = false
            onComplete()
        }
    }
    
    /**
     * Refresh the quote (force fetch).
     */
    fun refresh(onComplete: () -> Unit = {}) {
        fetchQuote(onComplete)
    }
    
    /**
     * Clear any error message.
     */
    fun clearError() {
        _errorMessage.value = null
    }
    
    /**
     * Try to refresh the access token.
     */
    private suspend fun tryRefreshToken(): Boolean {
        val refreshToken = tokenStorage.getRefreshToken() ?: return false
        
        return try {
            val result = authApiService.refreshToken(refreshToken)
            if (result is com.janusleaf.app.domain.model.AuthResult.Success) {
                tokenStorage.saveAccessToken(result.data.accessToken)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Napier.e("Token refresh failed", e, tag = "InspirationService")
            false
        }
    }
    
    // ==================== Observers for Swift ====================
    
    fun observeLoading(callback: (Boolean) -> Unit): Cancellable {
        val job = scope.launch {
            isLoadingFlow.collect { callback(it) }
        }
        return Cancellable { job.cancel() }
    }
    
    fun observeError(callback: (String?) -> Unit): Cancellable {
        val job = scope.launch {
            errorMessageFlow.collect { callback(it) }
        }
        return Cancellable { job.cancel() }
    }
    
    fun observeQuote(callback: (InspirationalQuote?) -> Unit): Cancellable {
        val job = scope.launch {
            quoteFlow.collect { callback(it) }
        }
        return Cancellable { job.cancel() }
    }
    
    fun observeNotFound(callback: (Boolean) -> Unit): Cancellable {
        val job = scope.launch {
            isNotFoundFlow.collect { callback(it) }
        }
        return Cancellable { job.cancel() }
    }
}

/**
 * Factory function to create the inspiration service.
 */
fun createInspirationService(): IosInspirationService = IosInspirationService()
