package com.janusleaf.model

import jakarta.persistence.*
import java.time.Instant
import java.util.*

/**
 * Queue entry for pending AI mood analysis.
 * 
 * Provides database-backed debouncing:
 * - When body is edited, a queue entry is created/updated
 * - scheduledFor is set to now + debounce delay
 * - Each edit resets scheduledFor (debouncing)
 * - A scheduled job processes entries where scheduledFor <= now
 */
@Entity
@Table(name = "mood_analysis_queue")
class MoodAnalysisQueue(
    @Id
    val id: UUID = UUID.randomUUID(),

    /**
     * The journal entry to analyze.
     * Unique constraint ensures only one pending analysis per entry.
     */
    @Column(name = "journal_entry_id", nullable = false, unique = true)
    val journalEntryId: UUID,

    /**
     * Snapshot of the body content to analyze.
     * Captured when queued so analysis uses the correct version.
     */
    @Column(name = "body_snapshot", columnDefinition = "TEXT", nullable = false)
    var bodySnapshot: String,

    /**
     * When this entry should be processed.
     * Updated on each edit to implement debouncing.
     */
    @Column(name = "scheduled_for", nullable = false)
    var scheduledFor: Instant,

    /**
     * Number of times this entry has been retried.
     * Used for exponential backoff on rate limit errors.
     */
    @Column(name = "retry_count", nullable = false)
    var retryCount: Int = 0,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()
) {
    @PreUpdate
    fun onUpdate() {
        updatedAt = Instant.now()
    }
}
