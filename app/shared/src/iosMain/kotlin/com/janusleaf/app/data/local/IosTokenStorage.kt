package com.janusleaf.app.data.local

import com.janusleaf.app.domain.repository.TokenStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import platform.Foundation.NSUserDefaults

class IosTokenStorage : TokenStorage {
    private val defaults: NSUserDefaults = NSUserDefaults.standardUserDefaults()
    private val hasTokensFlow = MutableStateFlow(hasTokensSync())

    override suspend fun saveAccessToken(token: String) = withContext(Dispatchers.Default) {
        defaults.setObject(token, forKey = KEY_ACCESS_TOKEN)
        defaults.synchronize()
        updateHasTokens()
    }

    override suspend fun getAccessToken(): String? = withContext(Dispatchers.Default) {
        defaults.stringForKey(KEY_ACCESS_TOKEN)
    }

    override suspend fun saveRefreshToken(token: String) = withContext(Dispatchers.Default) {
        defaults.setObject(token, forKey = KEY_REFRESH_TOKEN)
        defaults.synchronize()
        updateHasTokens()
    }

    override suspend fun getRefreshToken(): String? = withContext(Dispatchers.Default) {
        defaults.stringForKey(KEY_REFRESH_TOKEN)
    }

    override suspend fun clearTokens() = withContext(Dispatchers.Default) {
        defaults.removeObjectForKey(KEY_ACCESS_TOKEN)
        defaults.removeObjectForKey(KEY_REFRESH_TOKEN)
        defaults.synchronize()
        updateHasTokens()
    }

    override fun observeHasTokens(): Flow<Boolean> = hasTokensFlow.asStateFlow()

    override suspend fun hasTokens(): Boolean = withContext(Dispatchers.Default) {
        hasTokensSync()
    }

    private fun hasTokensSync(): Boolean {
        return defaults.stringForKey(KEY_ACCESS_TOKEN) != null &&
            defaults.stringForKey(KEY_REFRESH_TOKEN) != null
    }

    private fun updateHasTokens() {
        hasTokensFlow.value = hasTokensSync()
    }

    private companion object {
        const val KEY_ACCESS_TOKEN = "access_token"
        const val KEY_REFRESH_TOKEN = "refresh_token"
    }
}
