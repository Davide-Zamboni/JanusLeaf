package com.janusleaf.app.data.local

import com.janusleaf.app.domain.repository.TokenStorage
import io.github.aakira.napier.Napier
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import platform.Foundation.NSUserDefaults

/**
 * iOS implementation of TokenStorage using NSUserDefaults.
 * 
 * NOTE: For production apps, you should use Keychain for secure token storage.
 * This simplified implementation uses NSUserDefaults for compatibility.
 * Consider using a library like "multiplatform-settings" with encrypted settings
 * or implementing proper Keychain access for production use.
 */
class IosSecureTokenStorage : TokenStorage {
    
    private val defaults = NSUserDefaults.standardUserDefaults
    private val _hasTokensFlow = MutableStateFlow(false)
    
    init {
        // Initialize the flow with current state
        _hasTokensFlow.value = hasTokensSync()
    }
    
    override suspend fun saveAccessToken(token: String) = withContext(Dispatchers.IO) {
        defaults.setObject(token, KEY_ACCESS_TOKEN)
        defaults.synchronize()
        updateHasTokensFlow()
        Napier.d("Access token saved", tag = "TokenStorage")
    }
    
    override suspend fun getAccessToken(): String? = withContext(Dispatchers.IO) {
        defaults.stringForKey(KEY_ACCESS_TOKEN)
    }
    
    override suspend fun saveRefreshToken(token: String) = withContext(Dispatchers.IO) {
        defaults.setObject(token, KEY_REFRESH_TOKEN)
        defaults.synchronize()
        updateHasTokensFlow()
        Napier.d("Refresh token saved", tag = "TokenStorage")
    }
    
    override suspend fun getRefreshToken(): String? = withContext(Dispatchers.IO) {
        defaults.stringForKey(KEY_REFRESH_TOKEN)
    }
    
    override suspend fun clearTokens() = withContext(Dispatchers.IO) {
        defaults.removeObjectForKey(KEY_ACCESS_TOKEN)
        defaults.removeObjectForKey(KEY_REFRESH_TOKEN)
        defaults.synchronize()
        updateHasTokensFlow()
        Napier.d("Tokens cleared", tag = "TokenStorage")
    }
    
    override fun observeHasTokens(): Flow<Boolean> = _hasTokensFlow.asStateFlow()
    
    override suspend fun hasTokens(): Boolean = withContext(Dispatchers.IO) {
        hasTokensSync()
    }
    
    private fun hasTokensSync(): Boolean {
        return defaults.stringForKey(KEY_ACCESS_TOKEN) != null &&
               defaults.stringForKey(KEY_REFRESH_TOKEN) != null
    }
    
    private fun updateHasTokensFlow() {
        _hasTokensFlow.value = hasTokensSync()
    }
    
    companion object {
        private const val KEY_ACCESS_TOKEN = "com.janusleaf.access_token"
        private const val KEY_REFRESH_TOKEN = "com.janusleaf.refresh_token"
    }
}
