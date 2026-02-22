package com.janusleaf.app.domain.repository

/**
 * Clears app data that should not survive an auth/session reset.
 */
interface SessionDataRepository {
    suspend fun clearSessionData()
}
