package com.janusleaf.repository

import com.janusleaf.model.InspirationalQuote
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.*

@Repository
interface InspirationalQuoteRepository : JpaRepository<InspirationalQuote, UUID> {

    /**
     * Find quote for a specific user.
     */
    fun findByUserId(userId: UUID): InspirationalQuote?

    /**
     * Check if user has a quote.
     */
    fun existsByUserId(userId: UUID): Boolean

    /**
     * Find all quotes that need regeneration (flagged or older than 24 hours).
     */
    @Query("""
        SELECT q FROM InspirationalQuote q 
        WHERE q.needsRegeneration = true 
        OR q.lastGeneratedAt < :cutoffTime
        ORDER BY q.lastGeneratedAt ASC
    """)
    fun findQuotesNeedingRegeneration(cutoffTime: Instant): List<InspirationalQuote>

    /**
     * Mark a user's quote as needing regeneration.
     * Called when user creates a new journal entry.
     */
    @Modifying
    @Query("UPDATE InspirationalQuote q SET q.needsRegeneration = true, q.updatedAt = :now WHERE q.user.id = :userId")
    fun markForRegeneration(userId: UUID, now: Instant = Instant.now()): Int

    /**
     * Find all user IDs that don't have a quote yet but have journal entries.
     * Used to generate initial quotes for users.
     */
    @Query("""
        SELECT DISTINCT je.user.id FROM JournalEntry je 
        WHERE je.user.id NOT IN (SELECT q.user.id FROM InspirationalQuote q)
    """)
    fun findUserIdsWithoutQuotes(): List<UUID>

    /**
     * Delete quote for a user.
     */
    @Modifying
    @Query("DELETE FROM InspirationalQuote q WHERE q.user.id = :userId")
    fun deleteByUserId(userId: UUID): Int
}
