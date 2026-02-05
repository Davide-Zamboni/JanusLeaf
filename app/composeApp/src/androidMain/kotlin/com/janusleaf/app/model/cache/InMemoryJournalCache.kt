package com.janusleaf.app.model.cache

import com.janusleaf.app.domain.model.Journal
import com.janusleaf.app.domain.model.JournalPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class InMemoryJournalCache : JournalCache {
    private val entriesState = MutableStateFlow<List<JournalPreview>>(emptyList())
    private val entryState = MutableStateFlow<Map<String, Journal>>(emptyMap())

    override fun observeEntries(): Flow<List<JournalPreview>> = entriesState.asStateFlow()

    override fun observeEntry(entryId: String): Flow<Journal?> {
        return entryState
            .map { it[entryId] }
            .distinctUntilChanged()
    }

    override suspend fun getEntry(entryId: String): Journal? = entryState.value[entryId]

    override suspend fun getPreview(entryId: String): JournalPreview? {
        return entriesState.value.firstOrNull { it.id == entryId }
    }

    override suspend fun replaceEntries(entries: List<JournalPreview>) {
        entriesState.value = entries.distinctBy { it.id }
    }

    override suspend fun appendEntries(entries: List<JournalPreview>) {
        if (entries.isEmpty()) return
        entriesState.update { current ->
            val currentIds = current.asSequence().map { it.id }.toSet()
            val newItems = entries.filterNot { currentIds.contains(it.id) }
            if (newItems.isEmpty()) current else current + newItems
        }
    }

    override suspend fun upsertPreview(preview: JournalPreview) {
        entriesState.update { current ->
            val index = current.indexOfFirst { it.id == preview.id }
            if (index >= 0) {
                current.toMutableList().apply { set(index, preview) }
            } else {
                listOf(preview) + current
            }
        }
    }

    override suspend fun upsertEntry(entry: Journal) {
        entryState.update { current -> current + (entry.id to entry) }
    }

    override suspend fun removeEntry(entryId: String) {
        entryState.update { current -> current - entryId }
        entriesState.update { current -> current.filterNot { it.id == entryId } }
    }

    override suspend fun clear() {
        entriesState.value = emptyList()
        entryState.value = emptyMap()
    }
}
