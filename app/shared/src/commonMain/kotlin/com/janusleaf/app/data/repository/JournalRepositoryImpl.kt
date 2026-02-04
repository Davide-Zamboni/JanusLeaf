package com.janusleaf.app.data.repository

import com.janusleaf.app.data.remote.AuthApiService
import com.janusleaf.app.data.remote.JournalApiService
import com.janusleaf.app.domain.model.*
import com.janusleaf.app.domain.repository.JournalRepository
import com.janusleaf.app.domain.repository.TokenStorage
import io.github.aakira.napier.Napier
import kotlinx.datetime.LocalDate

/**
 * Implementation of JournalRepository.
 * Coordinates between the API service and token storage for authenticated requests.
 * Handles automatic token refresh on 401 errors.
 */
class JournalRepositoryImpl(
    private val apiService: JournalApiService,
    private val tokenStorage: TokenStorage,
    private val authApiService: AuthApiService? = null // Optional for token refresh
) : JournalRepository {
    
    private suspend fun getAccessToken(): String? = tokenStorage.getAccessToken()
    
    /**
     * Try to refresh the access token using the refresh token.
     * Returns the new access token if successful, null otherwise.
     */
    private suspend fun tryRefreshToken(): String? {
        val authApi = authApiService ?: return null
        val refreshToken = tokenStorage.getRefreshToken() ?: return null
        
        return when (val result = authApi.refreshToken(refreshToken)) {
            is AuthResult.Success -> {
                val newAccessToken = result.data.accessToken
                tokenStorage.saveAccessToken(newAccessToken)
                Napier.d("Token refreshed successfully in JournalRepository", tag = "JournalRepository")
                newAccessToken
            }
            is AuthResult.Error -> {
                Napier.e("Token refresh failed: ${result.error}", tag = "JournalRepository")
                // Clear tokens on refresh failure (refresh token likely expired)
                tokenStorage.clearTokens()
                null
            }
            is AuthResult.Loading -> null
        }
    }
    
    /**
     * Execute an API call with automatic token refresh on Unauthorized errors.
     */
    private suspend fun <T> withTokenRefresh(
        apiCall: suspend (String) -> JournalResult<T>
    ): JournalResult<T> {
        val accessToken = getAccessToken()
            ?: return JournalResult.Error(JournalError.Unauthorized)
        
        return when (val result = apiCall(accessToken)) {
            is JournalResult.Error -> {
                if (result.error == JournalError.Unauthorized) {
                    // Try to refresh token and retry
                    val newToken = tryRefreshToken()
                    if (newToken != null) {
                        Napier.d("Retrying API call with refreshed token", tag = "JournalRepository")
                        apiCall(newToken)
                    } else {
                        result
                    }
                } else {
                    result
                }
            }
            else -> result
        }
    }
    
    override suspend fun createEntry(
        title: String?,
        body: String?,
        entryDate: LocalDate?
    ): JournalResult<Journal> = withTokenRefresh { token ->
        when (val result = apiService.createEntry(token, title, body, entryDate)) {
            is JournalResult.Success -> {
                val journal = result.data.toDomain()
                Napier.d("Journal entry created: ${journal.id}", tag = "JournalRepository")
                JournalResult.Success(journal)
            }
            is JournalResult.Error -> result
            is JournalResult.Loading -> result
        }
    }
    
    override suspend fun getEntry(id: String): JournalResult<Journal> = withTokenRefresh { token ->
        when (val result = apiService.getEntry(token, id)) {
            is JournalResult.Success -> {
                JournalResult.Success(result.data.toDomain())
            }
            is JournalResult.Error -> result
            is JournalResult.Loading -> result
        }
    }
    
    override suspend fun listEntries(page: Int, size: Int): JournalResult<JournalPage> = withTokenRefresh { token ->
        when (val result = apiService.listEntries(token, page, size)) {
            is JournalResult.Success -> {
                val journalPage = JournalPage(
                    entries = result.data.entries.map { it.toDomain() },
                    page = result.data.page,
                    size = result.data.size,
                    totalElements = result.data.totalElements,
                    totalPages = result.data.totalPages,
                    hasNext = result.data.hasNext,
                    hasPrevious = result.data.hasPrevious
                )
                JournalResult.Success(journalPage)
            }
            is JournalResult.Error -> result
            is JournalResult.Loading -> result
        }
    }
    
    override suspend fun getEntriesByDateRange(
        startDate: LocalDate,
        endDate: LocalDate
    ): JournalResult<List<JournalPreview>> = withTokenRefresh { token ->
        when (val result = apiService.getEntriesByDateRange(token, startDate, endDate)) {
            is JournalResult.Success -> {
                JournalResult.Success(result.data.map { it.toDomain() })
            }
            is JournalResult.Error -> result
            is JournalResult.Loading -> result
        }
    }
    
    override suspend fun updateBody(
        id: String,
        body: String,
        expectedVersion: Long?
    ): JournalResult<JournalBodyUpdate> = withTokenRefresh { token ->
        when (val result = apiService.updateBody(token, id, body, expectedVersion)) {
            is JournalResult.Success -> {
                val update = JournalBodyUpdate(
                    id = result.data.id,
                    body = result.data.body,
                    version = result.data.version,
                    updatedAt = result.data.updatedAt
                )
                Napier.d("Journal body updated: $id (v${update.version})", tag = "JournalRepository")
                JournalResult.Success(update)
            }
            is JournalResult.Error -> result
            is JournalResult.Loading -> result
        }
    }
    
    override suspend fun updateMetadata(
        id: String,
        title: String?,
        moodScore: Int?,
        expectedVersion: Long?
    ): JournalResult<Journal> = withTokenRefresh { token ->
        when (val result = apiService.updateMetadata(token, id, title, moodScore, expectedVersion)) {
            is JournalResult.Success -> {
                val journal = result.data.toDomain()
                Napier.d("Journal metadata updated: $id", tag = "JournalRepository")
                JournalResult.Success(journal)
            }
            is JournalResult.Error -> result
            is JournalResult.Loading -> result
        }
    }
    
    override suspend fun deleteEntry(id: String): JournalResult<Unit> = withTokenRefresh { token ->
        when (val result = apiService.deleteEntry(token, id)) {
            is JournalResult.Success -> {
                Napier.d("Journal entry deleted: $id", tag = "JournalRepository")
                JournalResult.Success(Unit)
            }
            is JournalResult.Error -> result
            is JournalResult.Loading -> result
        }
    }
}
