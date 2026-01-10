package com.janusleaf.repository

import com.janusleaf.model.JournalEntry
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.util.*

@Repository
interface JournalEntryRepository : JpaRepository<JournalEntry, UUID> {
    
    fun findByUserId(userId: UUID, pageable: Pageable): Page<JournalEntry>
    
    fun findByUserIdAndId(userId: UUID, id: UUID): JournalEntry?
    
    fun findByUserIdAndEntryDate(userId: UUID, entryDate: LocalDate): JournalEntry?
    
    fun findByUserIdAndEntryDateBetweenOrderByEntryDateDescUpdatedAtDesc(
        userId: UUID, 
        startDate: LocalDate, 
        endDate: LocalDate
    ): List<JournalEntry>
    
    fun existsByUserIdAndId(userId: UUID, id: UUID): Boolean
    
    fun deleteByUserIdAndId(userId: UUID, id: UUID): Long
    
    /**
     * Reset mood score to null for a journal entry.
     * Uses native query to ensure the null value is properly persisted.
     */
    @Modifying
    @Query("UPDATE journal_entries SET mood_score = NULL WHERE id = :entryId", nativeQuery = true)
    fun resetMoodScore(entryId: UUID): Int
}
