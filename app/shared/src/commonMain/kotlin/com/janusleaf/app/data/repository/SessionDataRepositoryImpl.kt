package com.janusleaf.app.data.repository

import com.janusleaf.app.domain.repository.SessionDataRepository
import com.janusleaf.app.model.cache.InspirationCache
import com.janusleaf.app.model.cache.JournalCache

class SessionDataRepositoryImpl(
    private val journalCache: JournalCache,
    private val inspirationCache: InspirationCache
) : SessionDataRepository {

    override suspend fun clearSessionData() {
        journalCache.clear()
        inspirationCache.clear()
    }
}
