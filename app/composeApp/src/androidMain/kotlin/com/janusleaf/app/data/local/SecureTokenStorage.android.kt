package com.janusleaf.app.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.janusleaf.app.domain.repository.TokenStorage
import io.github.aakira.napier.Napier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/**
 * Android implementation of TokenStorage using EncryptedSharedPreferences.
 * Uses Android Keystore for secure key storage.
 */
class AndroidSecureTokenStorage(context: Context) : TokenStorage {
    
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()
    
    private val encryptedPrefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        PREFS_FILE_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    
    private val _hasTokensFlow = MutableStateFlow(hasTokensSync())
    
    override suspend fun saveAccessToken(token: String) = withContext(Dispatchers.IO) {
        encryptedPrefs.edit().putString(KEY_ACCESS_TOKEN, token).apply()
        updateHasTokensFlow()
        Napier.d("Access token saved", tag = "TokenStorage")
    }
    
    override suspend fun getAccessToken(): String? = withContext(Dispatchers.IO) {
        encryptedPrefs.getString(KEY_ACCESS_TOKEN, null)
    }
    
    override suspend fun saveRefreshToken(token: String) = withContext(Dispatchers.IO) {
        encryptedPrefs.edit().putString(KEY_REFRESH_TOKEN, token).apply()
        updateHasTokensFlow()
        Napier.d("Refresh token saved", tag = "TokenStorage")
    }
    
    override suspend fun getRefreshToken(): String? = withContext(Dispatchers.IO) {
        encryptedPrefs.getString(KEY_REFRESH_TOKEN, null)
    }
    
    override suspend fun clearTokens() = withContext(Dispatchers.IO) {
        encryptedPrefs.edit()
            .remove(KEY_ACCESS_TOKEN)
            .remove(KEY_REFRESH_TOKEN)
            .apply()
        updateHasTokensFlow()
        Napier.d("Tokens cleared", tag = "TokenStorage")
    }
    
    override fun observeHasTokens(): Flow<Boolean> = _hasTokensFlow.asStateFlow()
    
    override suspend fun hasTokens(): Boolean = withContext(Dispatchers.IO) {
        hasTokensSync()
    }
    
    private fun hasTokensSync(): Boolean {
        return encryptedPrefs.getString(KEY_ACCESS_TOKEN, null) != null &&
               encryptedPrefs.getString(KEY_REFRESH_TOKEN, null) != null
    }
    
    private fun updateHasTokensFlow() {
        _hasTokensFlow.value = hasTokensSync()
    }
    
    companion object {
        private const val PREFS_FILE_NAME = "janusleaf_secure_prefs"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
    }
}
