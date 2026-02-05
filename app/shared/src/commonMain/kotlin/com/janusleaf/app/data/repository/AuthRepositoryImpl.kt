package com.janusleaf.app.data.repository

import com.janusleaf.app.data.remote.AuthApiService
import com.janusleaf.app.domain.model.AuthError
import com.janusleaf.app.domain.model.AuthResult
import com.janusleaf.app.domain.model.AuthenticatedUser
import com.janusleaf.app.domain.model.RefreshedToken
import com.janusleaf.app.domain.model.User
import com.janusleaf.app.domain.repository.AuthRepository
import com.janusleaf.app.domain.repository.TokenStorage
import io.github.aakira.napier.Napier
import kotlinx.coroutines.flow.Flow

/**
 * Implementation of AuthRepository.
 * Coordinates between the API service and token storage.
 */
class AuthRepositoryImpl(
    private val apiService: AuthApiService,
    private val tokenStorage: TokenStorage
) : AuthRepository {
    
    override suspend fun register(
        email: String,
        username: String,
        password: String
    ): AuthResult<AuthenticatedUser> {
        return when (val result = apiService.register(email, username, password)) {
            is AuthResult.Success -> {
                val authenticatedUser = result.data.toDomain()
                // Store tokens securely
                tokenStorage.saveAccessToken(authenticatedUser.tokens.accessToken)
                tokenStorage.saveRefreshToken(authenticatedUser.tokens.refreshToken)
                Napier.d("User registered successfully: ${authenticatedUser.user.email}", tag = "AuthRepository")
                AuthResult.Success(authenticatedUser)
            }
            is AuthResult.Error -> result
            is AuthResult.Loading -> result
        }
    }
    
    override suspend fun login(
        email: String,
        password: String
    ): AuthResult<AuthenticatedUser> {
        return when (val result = apiService.login(email, password)) {
            is AuthResult.Success -> {
                val authenticatedUser = result.data.toDomain()
                // Store tokens securely
                tokenStorage.saveAccessToken(authenticatedUser.tokens.accessToken)
                tokenStorage.saveRefreshToken(authenticatedUser.tokens.refreshToken)
                Napier.d("User logged in successfully: ${authenticatedUser.user.email}", tag = "AuthRepository")
                AuthResult.Success(authenticatedUser)
            }
            is AuthResult.Error -> result
            is AuthResult.Loading -> result
        }
    }
    
    override suspend fun refreshToken(refreshToken: String): AuthResult<RefreshedToken> {
        return when (val result = apiService.refreshToken(refreshToken)) {
            is AuthResult.Success -> {
                val refreshedToken = result.data.toDomain()
                // Update stored access token
                tokenStorage.saveAccessToken(refreshedToken.accessToken)
                Napier.d("Token refreshed successfully", tag = "AuthRepository")
                AuthResult.Success(refreshedToken)
            }
            is AuthResult.Error -> result
            is AuthResult.Loading -> result
        }
    }
    
    override suspend fun logout(refreshToken: String): AuthResult<Unit> {
        // Try to logout from server, but clear local tokens regardless
        val result = apiService.logout(refreshToken)
        clearAuthData()
        Napier.d("Logged out", tag = "AuthRepository")
        return when (result) {
            is AuthResult.Success -> AuthResult.Success(Unit)
            is AuthResult.Error -> AuthResult.Success(Unit) // Clear tokens even on error
            is AuthResult.Loading -> AuthResult.Loading
        }
    }
    
    override suspend fun logoutAll(): AuthResult<Unit> {
        val accessToken = tokenStorage.getAccessToken()
            ?: return AuthResult.Error(AuthError.InvalidToken)
        
        return when (val result = apiService.logoutAll(accessToken)) {
            is AuthResult.Success -> {
                clearAuthData()
                Napier.d("Logged out from all devices", tag = "AuthRepository")
                AuthResult.Success(Unit)
            }
            is AuthResult.Error -> result
            is AuthResult.Loading -> result
        }
    }
    
    override suspend fun getCurrentUser(): AuthResult<User> {
        val accessToken = tokenStorage.getAccessToken()
            ?: return AuthResult.Error(AuthError.InvalidToken)
        
        return when (val result = apiService.getCurrentUser(accessToken)) {
            is AuthResult.Success -> AuthResult.Success(result.data.toDomain())
            is AuthResult.Error -> {
                // If token is expired, try to refresh
                if (result.error is AuthError.TokenExpired || result.error is AuthError.InvalidToken) {
                    val refreshResult = tryRefreshToken()
                    if (refreshResult) {
                        // Retry with new token
                        val newAccessToken = tokenStorage.getAccessToken()
                            ?: return AuthResult.Error(AuthError.InvalidToken)
                        return when (val retryResult = apiService.getCurrentUser(newAccessToken)) {
                            is AuthResult.Success -> AuthResult.Success(retryResult.data.toDomain())
                            is AuthResult.Error -> retryResult
                            is AuthResult.Loading -> retryResult
                        }
                    }
                }
                result
            }
            is AuthResult.Loading -> result
        }
    }
    
    override suspend fun updateProfile(username: String): AuthResult<User> {
        val accessToken = tokenStorage.getAccessToken()
            ?: return AuthResult.Error(AuthError.InvalidToken)
        
        return when (val result = apiService.updateProfile(accessToken, username)) {
            is AuthResult.Success -> AuthResult.Success(result.data.toDomain())
            is AuthResult.Error -> result
            is AuthResult.Loading -> result
        }
    }
    
    override suspend fun changePassword(
        currentPassword: String,
        newPassword: String
    ): AuthResult<Unit> {
        val accessToken = tokenStorage.getAccessToken()
            ?: return AuthResult.Error(AuthError.InvalidToken)
        
        return when (val result = apiService.changePassword(accessToken, currentPassword, newPassword)) {
            is AuthResult.Success -> {
                clearAuthData()
                Napier.d("Password changed, tokens cleared", tag = "AuthRepository")
                AuthResult.Success(Unit)
            }
            is AuthResult.Error -> result
            is AuthResult.Loading -> result
        }
    }
    
    override fun observeAuthState(): Flow<Boolean> = tokenStorage.observeHasTokens()
    
    override suspend fun isAuthenticated(): Boolean = tokenStorage.hasTokens()
    
    override suspend fun clearAuthData() {
        tokenStorage.clearTokens()
    }
    
    /**
     * Try to refresh the access token.
     * Returns true if successful, false otherwise.
     */
    private suspend fun tryRefreshToken(): Boolean {
        val refreshToken = tokenStorage.getRefreshToken() ?: return false
        return when (val result = refreshToken(refreshToken)) {
            is AuthResult.Success -> true
            else -> {
                clearAuthData()
                false
            }
        }
    }
}
