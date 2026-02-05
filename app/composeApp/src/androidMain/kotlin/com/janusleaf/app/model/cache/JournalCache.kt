package com.janusleaf.app.model.cache

import com.janusleaf.app.domain.model.Journal
import com.janusleaf.app.domain.model.JournalPreview
import kotlinx.coroutines.flow.Flow

interface JournalCache {
    fun observeEntries(): Flow<List<JournalPreview>>

    fun observeEntry(entryId: String): Flow<Journal?>

    suspend fun getEntry(entryId: String): Journal?

    suspend fun getPreview(entryId: String): JournalPreview?

    suspend fun replaceEntries(entries: List<JournalPreview>)

    suspend fun appendEntries(entries: List<JournalPreview>)

    suspend fun upsertPreview(preview: JournalPreview)

    suspend fun upsertEntry(entry: Journal)

    suspend fun removeEntry(entryId: String)

    suspend fun clear()
}
