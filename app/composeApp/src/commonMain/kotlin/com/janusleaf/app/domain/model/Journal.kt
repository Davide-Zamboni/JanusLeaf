package com.janusleaf.app.domain.model

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate

/**
 * Domain model representing a full journal entry.
 */
data class Journal(
    val id: String,
    val title: String,
    val body: String,
    val moodScore: Int?,
    val entryDate: LocalDate,
    val version: Long,
    val createdAt: Instant,
    val updatedAt: Instant
)

/**
 * Domain model representing a journal entry preview (for list views).
 */
data class JournalPreview(
    val id: String,
    val title: String,
    val bodyPreview: String,
    val moodScore: Int?,
    val entryDate: LocalDate,
    val updatedAt: Instant
)

/**
 * Paginated list of journal previews.
 */
data class JournalPage(
    val entries: List<JournalPreview>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
    val hasNext: Boolean,
    val hasPrevious: Boolean
)

/**
 * Result of a body update operation.
 */
data class JournalBodyUpdate(
    val id: String,
    val body: String,
    val version: Long,
    val updatedAt: Instant
)
