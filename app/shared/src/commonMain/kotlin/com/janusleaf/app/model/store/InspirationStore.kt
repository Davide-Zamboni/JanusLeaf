package com.janusleaf.app.model.store

import com.janusleaf.app.model.cache.InspirationCache
import com.janusleaf.app.data.remote.AuthApiService
import com.janusleaf.app.data.remote.InspirationApiService
import com.janusleaf.app.domain.model.AuthResult
import com.janusleaf.app.domain.model.InspirationError
import com.janusleaf.app.domain.model.InspirationResult
import com.janusleaf.app.domain.model.InspirationalQuote
import com.janusleaf.app.domain.repository.TokenStorage
import kotlinx.coroutines.flow.Flow

class InspirationStore(
    private val inspirationApiService: InspirationApiService,
    private val authApiService: AuthApiService,
    private val tokenStorage: TokenStorage,
    private val inspirationCache: InspirationCache
) {

    fun observeQuote(): Flow<InspirationalQuote?> = inspirationCache.observeQuote()

    suspend fun fetchQuote(): InspirationResult<InspirationalQuote> {
        val accessToken = tokenStorage.getAccessToken()
        if (accessToken == null) {
            return InspirationResult.Error(InspirationError.Unauthorized)
        }

        return when (val result = inspirationApiService.getInspiration(accessToken)) {
            is InspirationResult.Success -> {
                val quote = result.data.toDomain()
                inspirationCache.setQuote(quote)
                InspirationResult.Success(quote)
            }
            is InspirationResult.Error -> handleInspirationError(result.error)
            is InspirationResult.Loading -> InspirationResult.Loading
        }
    }

    suspend fun clear() {
        inspirationCache.clear()
    }

    private suspend fun handleInspirationError(error: InspirationError): InspirationResult<InspirationalQuote> {
        return when (error) {
            is InspirationError.Unauthorized -> {
                if (tryRefreshToken()) {
                    val newToken = tokenStorage.getAccessToken()
                    if (newToken == null) {
                        InspirationResult.Error(InspirationError.Unauthorized)
                    } else {
                        when (val retry = inspirationApiService.getInspiration(newToken)) {
                            is InspirationResult.Success -> {
                                val quote = retry.data.toDomain()
                                inspirationCache.setQuote(quote)
                                InspirationResult.Success(quote)
                            }
                            is InspirationResult.Error -> handleNonAuthError(retry.error)
                            is InspirationResult.Loading -> InspirationResult.Loading
                        }
                    }
                } else {
                    InspirationResult.Error(InspirationError.Unauthorized)
                }
            }
            else -> handleNonAuthError(error)
        }
    }

    private suspend fun handleNonAuthError(error: InspirationError): InspirationResult<InspirationalQuote> {
        if (error is InspirationError.NotFound) {
            inspirationCache.setQuote(null)
        }
        return InspirationResult.Error(error)
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
