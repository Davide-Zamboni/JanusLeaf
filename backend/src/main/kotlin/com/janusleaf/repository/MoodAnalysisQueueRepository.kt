package com.janusleaf.repository

import com.janusleaf.model.MoodAnalysisQueue
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.*

@Repository
interface MoodAnalysisQueueRepository : JpaRepository<MoodAnalysisQueue, UUID> {

    /**
     * Find existing queue entry for a journal entry.
     */
    fun findByJournalEntryId(journalEntryId: UUID): MoodAnalysisQueue?

    /**
     * Find all entries ready to be processed (scheduled time has passed).
     * Limited to avoid processing too many at once.
     */
    @Query("""
        SELECT q FROM MoodAnalysisQueue q 
        WHERE q.scheduledFor <= :now 
        ORDER BY q.scheduledFor ASC
    """)
    fun findReadyToProcess(now: Instant): List<MoodAnalysisQueue>

    /**
     * Delete queue entry for a journal entry.
     * Used when entry is deleted or after successful processing.
     */
    @Modifying
    @Query("DELETE FROM MoodAnalysisQueue q WHERE q.journalEntryId = :journalEntryId")
    fun deleteByJournalEntryId(journalEntryId: UUID): Int

    /**
     * Check if there's a pending analysis for a journal entry.
     */
    fun existsByJournalEntryId(journalEntryId: UUID): Boolean
}
