package com.janusleaf.app.model.store

import com.janusleaf.app.model.cache.JournalCache
import com.janusleaf.app.domain.model.Journal
import com.janusleaf.app.domain.model.JournalBodyUpdate
import com.janusleaf.app.domain.model.JournalPage
import com.janusleaf.app.domain.model.JournalPreview
import com.janusleaf.app.domain.model.JournalResult
import com.janusleaf.app.domain.repository.JournalRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

private const val DEFAULT_PAGE_SIZE = 20

class JournalStore(
    private val journalRepository: JournalRepository,
    private val journalCache: JournalCache
) {

    fun observeEntries(): Flow<List<JournalPreview>> = journalCache.observeEntries()

    fun observeEntry(entryId: String): Flow<Journal?> = journalCache.observeEntry(entryId)

    suspend fun loadEntries(page: Int = 0, size: Int = DEFAULT_PAGE_SIZE): JournalResult<JournalPage> {
        val result = journalRepository.listEntries(page = page, size = size)
        if (result is JournalResult.Success) {
            if (page == 0) {
                journalCache.replaceEntries(result.data.entries)
            } else {
                journalCache.appendEntries(result.data.entries)
            }
        }
        return result
    }

    suspend fun createEntry(
        title: String? = null,
        body: String? = null,
        entryDate: LocalDate? = null
    ): JournalResult<Journal> {
        val result = journalRepository.createEntry(title, body, entryDate)
        if (result is JournalResult.Success) {
            journalCache.upsertEntry(result.data)
            journalCache.upsertPreview(result.data.toPreview())
        }
        return result
    }

    suspend fun getEntry(id: String): JournalResult<Journal> {
        val result = journalRepository.getEntry(id)
        if (result is JournalResult.Success) {
            journalCache.upsertEntry(result.data)
            journalCache.upsertPreview(result.data.toPreview())
        }
        return result
    }

    suspend fun updateBody(
        id: String,
        body: String,
        expectedVersion: Long? = null
    ): JournalResult<JournalBodyUpdate> {
        val result = journalRepository.updateBody(id, body, expectedVersion)
        if (result is JournalResult.Success) {
            applyBodyUpdate(result.data)
        }
        return result
    }

    suspend fun updateMetadata(
        id: String,
        title: String? = null,
        moodScore: Int? = null,
        expectedVersion: Long? = null
    ): JournalResult<Journal> {
        val result = journalRepository.updateMetadata(id, title, moodScore, expectedVersion)
        if (result is JournalResult.Success) {
            journalCache.upsertEntry(result.data)
            journalCache.upsertPreview(result.data.toPreview())
        }
        return result
    }

    suspend fun deleteEntry(id: String): JournalResult<Unit> {
        val result = journalRepository.deleteEntry(id)
        if (result is JournalResult.Success) {
            journalCache.removeEntry(id)
        }
        return result
    }

    suspend fun clearAll() {
        journalCache.clear()
    }

    private suspend fun applyBodyUpdate(update: JournalBodyUpdate) {
        val existingEntry = journalCache.getEntry(update.id)
        if (existingEntry != null) {
            val updatedEntry = existingEntry.copy(
                body = update.body,
                version = update.version,
                updatedAt = update.updatedAt
            )
            journalCache.upsertEntry(updatedEntry)
            journalCache.upsertPreview(updatedEntry.toPreview().copy(moodScore = null))
            return
        }

        val existingPreview = journalCache.getPreview(update.id)
        if (existingPreview != null) {
            journalCache.upsertPreview(
                existingPreview.copy(
                    bodyPreview = update.body.take(150),
                    moodScore = null,
                    updatedAt = update.updatedAt
                )
            )
        }
    }

    private fun Journal.toPreview(): JournalPreview = JournalPreview(
        id = id,
        title = title,
        bodyPreview = body.take(150),
        moodScore = moodScore,
        entryDate = entryDate,
        updatedAt = updatedAt
    )
}
