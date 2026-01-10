package com.janusleaf.dto

import jakarta.validation.constraints.Size
import java.time.Instant
import java.time.LocalDate
import java.util.*

// ==================== Request DTOs ====================

/**
 * Request to create a new journal entry.
 * Title is optional - defaults to today's date if not provided.
 */
data class CreateJournalEntryRequest(
    @field:Size(max = 255, message = "Title must not exceed 255 characters")
    val title: String? = null,

    @field:Size(max = 50000, message = "Body must not exceed 50000 characters")
    val body: String? = null,

    val entryDate: LocalDate? = null
)

/**
 * Request to update journal entry body content.
 * Supports Google Docs-like incremental updates with version control.
 */
data class UpdateJournalBodyRequest(
    @field:Size(max = 50000, message = "Body must not exceed 50000 characters")
    val body: String,

    /**
     * Optional version for optimistic locking.
     * If provided, update will fail if version doesn't match (concurrent edit detected).
     */
    val expectedVersion: Long? = null
)

/**
 * Request to update journal entry metadata (title only).
 * Note: mood_score is AI-generated and cannot be set by users.
 */
data class UpdateJournalMetadataRequest(
    @field:Size(max = 255, message = "Title must not exceed 255 characters")
    val title: String? = null
)

// ==================== Response DTOs ====================

/**
 * Full journal entry response with all details.
 */
data class JournalEntryResponse(
    val id: UUID,
    val title: String,
    val body: String,
    val moodScore: Int?,
    val entryDate: LocalDate,
    val version: Long,
    val createdAt: Instant,
    val updatedAt: Instant
)

/**
 * Lightweight journal entry response for list views.
 */
data class JournalEntrySummaryResponse(
    val id: UUID,
    val title: String,
    val bodyPreview: String,
    val moodScore: Int?,
    val entryDate: LocalDate,
    val updatedAt: Instant
)

/**
 * Paginated list of journal entries.
 */
data class JournalEntriesPageResponse(
    val entries: List<JournalEntrySummaryResponse>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
    val hasNext: Boolean,
    val hasPrevious: Boolean
)

/**
 * Response after updating body content.
 * Returns updated version for optimistic locking.
 */
data class JournalBodyUpdateResponse(
    val id: UUID,
    val body: String,
    val version: Long,
    val updatedAt: Instant
)
