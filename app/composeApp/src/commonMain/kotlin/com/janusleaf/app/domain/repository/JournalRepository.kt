package com.janusleaf.app.domain.repository

import com.janusleaf.app.domain.model.*
import kotlinx.datetime.LocalDate

/**
 * Repository interface for journal operations.
 * Provides a clean API for the presentation layer to interact with journal data.
 */
interface JournalRepository {
    /**
     * Create a new journal entry.
     * @param title Optional title (defaults to entry date if not provided)
     * @param body Optional initial body content
     * @param entryDate Optional date for the entry (defaults to today)
     */
    suspend fun createEntry(
        title: String? = null,
        body: String? = null,
        entryDate: LocalDate? = null
    ): JournalResult<Journal>
    
    /**
     * Get a journal entry by ID.
     */
    suspend fun getEntry(id: String): JournalResult<Journal>
    
    /**
     * Get paginated list of journal entries.
     * @param page Page number (0-indexed)
     * @param size Page size (default 20, max 100)
     */
    suspend fun listEntries(
        page: Int = 0,
        size: Int = 20
    ): JournalResult<JournalPage>
    
    /**
     * Get journal entries within a date range.
     * @param startDate Start date (inclusive)
     * @param endDate End date (inclusive)
     */
    suspend fun getEntriesByDateRange(
        startDate: LocalDate,
        endDate: LocalDate
    ): JournalResult<List<JournalPreview>>
    
    /**
     * Update the body content of a journal entry.
     * Supports optimistic locking for concurrent edit detection.
     * @param id Entry ID
     * @param body New body content
     * @param expectedVersion Optional expected version for optimistic locking
     */
    suspend fun updateBody(
        id: String,
        body: String,
        expectedVersion: Long? = null
    ): JournalResult<JournalBodyUpdate>
    
    /**
     * Update metadata (title, moodScore) of a journal entry.
     * @param id Entry ID
     * @param title New title (null to keep unchanged)
     * @param moodScore New mood score 1-10 (null to keep unchanged)
     */
    suspend fun updateMetadata(
        id: String,
        title: String? = null,
        moodScore: Int? = null
    ): JournalResult<Journal>
    
    /**
     * Delete a journal entry.
     */
    suspend fun deleteEntry(id: String): JournalResult<Unit>
}
