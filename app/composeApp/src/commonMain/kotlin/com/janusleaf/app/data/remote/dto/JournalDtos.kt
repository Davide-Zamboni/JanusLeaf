package com.janusleaf.app.data.remote.dto

import com.janusleaf.app.domain.model.Journal
import com.janusleaf.app.domain.model.JournalPreview
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

// ==================== Request DTOs ====================

@Serializable
data class CreateJournalRequestDto(
    val title: String? = null,
    val body: String? = null,
    val entryDate: LocalDate? = null
)

@Serializable
data class UpdateJournalBodyRequestDto(
    val body: String,
    val expectedVersion: Long? = null
)

@Serializable
data class UpdateJournalMetadataRequestDto(
    val title: String? = null,
    val moodScore: Int? = null
)

// ==================== Response DTOs ====================

@Serializable
data class JournalResponseDto(
    val id: String,
    val title: String,
    val body: String,
    val moodScore: Int? = null,
    val entryDate: LocalDate,
    val version: Long,
    val createdAt: Instant,
    val updatedAt: Instant
) {
    fun toDomain(): Journal = Journal(
        id = id,
        title = title,
        body = body,
        moodScore = moodScore,
        entryDate = entryDate,
        version = version,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

@Serializable
data class JournalPreviewResponseDto(
    val id: String,
    val title: String,
    val bodyPreview: String,
    val moodScore: Int? = null,
    val entryDate: LocalDate,
    val updatedAt: Instant
) {
    fun toDomain(): JournalPreview = JournalPreview(
        id = id,
        title = title,
        bodyPreview = bodyPreview,
        moodScore = moodScore,
        entryDate = entryDate,
        updatedAt = updatedAt
    )
}

@Serializable
data class JournalListResponseDto(
    val entries: List<JournalPreviewResponseDto>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
    val hasNext: Boolean,
    val hasPrevious: Boolean
)

@Serializable
data class JournalBodyUpdateResponseDto(
    val id: String,
    val body: String,
    val version: Long,
    val updatedAt: Instant
)
