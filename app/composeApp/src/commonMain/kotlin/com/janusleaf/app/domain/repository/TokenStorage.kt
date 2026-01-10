package com.janusleaf.app.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * Interface for secure token storage.
 * Platform-specific implementations will use secure storage mechanisms.
 */
interface TokenStorage {
    
    /**
     * Store the access token securely.
     */
    suspend fun saveAccessToken(token: String)
    
    /**
     * Get the stored access token.
     */
    suspend fun getAccessToken(): String?
    
    /**
     * Store the refresh token securely.
     */
    suspend fun saveRefreshToken(token: String)
    
    /**
     * Get the stored refresh token.
     */
    suspend fun getRefreshToken(): String?
    
    /**
     * Clear all stored tokens.
     */
    suspend fun clearTokens()
    
    /**
     * Observe whether tokens exist.
     */
    fun observeHasTokens(): Flow<Boolean>
    
    /**
     * Check if tokens are stored.
     */
    suspend fun hasTokens(): Boolean
}
