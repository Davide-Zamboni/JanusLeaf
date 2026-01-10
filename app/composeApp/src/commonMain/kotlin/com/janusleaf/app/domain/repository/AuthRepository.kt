package com.janusleaf.app.domain.repository

import com.janusleaf.app.domain.model.AuthResult
import com.janusleaf.app.domain.model.AuthenticatedUser
import com.janusleaf.app.domain.model.RefreshedToken
import com.janusleaf.app.domain.model.User
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for authentication operations.
 * Defines the contract for auth data operations following Clean Architecture.
 */
interface AuthRepository {
    
    /**
     * Register a new user account.
     */
    suspend fun register(
        email: String,
        username: String,
        password: String
    ): AuthResult<AuthenticatedUser>
    
    /**
     * Login with email and password.
     */
    suspend fun login(
        email: String,
        password: String
    ): AuthResult<AuthenticatedUser>
    
    /**
     * Refresh the access token using a refresh token.
     */
    suspend fun refreshToken(refreshToken: String): AuthResult<RefreshedToken>
    
    /**
     * Logout from current device (revoke refresh token).
     */
    suspend fun logout(refreshToken: String): AuthResult<Unit>
    
    /**
     * Logout from all devices.
     */
    suspend fun logoutAll(): AuthResult<Unit>
    
    /**
     * Get current user profile.
     */
    suspend fun getCurrentUser(): AuthResult<User>
    
    /**
     * Update user profile.
     */
    suspend fun updateProfile(username: String): AuthResult<User>
    
    /**
     * Change password (revokes all sessions).
     */
    suspend fun changePassword(
        currentPassword: String,
        newPassword: String
    ): AuthResult<Unit>
    
    /**
     * Observe the current authentication state.
     */
    fun observeAuthState(): Flow<Boolean>
    
    /**
     * Check if user is currently authenticated.
     */
    suspend fun isAuthenticated(): Boolean
    
    /**
     * Clear all stored authentication data.
     */
    suspend fun clearAuthData()
}
