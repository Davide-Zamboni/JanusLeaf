package com.janusleaf.service

import com.janusleaf.dto.*
import com.janusleaf.exception.NoteAccessDeniedException
import com.janusleaf.exception.NoteNotFoundException
import com.janusleaf.model.JournalEntry
import com.janusleaf.model.User
import com.janusleaf.repository.JournalEntryRepository
import com.janusleaf.repository.UserRepository
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.orm.ObjectOptimisticLockingFailureException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.*

@Service
class JournalService(
    private val journalEntryRepository: JournalEntryRepository,
    private val userRepository: UserRepository,
    private val moodAnalysisService: MoodAnalysisService
) {

    companion object {
        private const val BODY_PREVIEW_LENGTH = 150
    }

    /**
     * Create a new journal entry.
     * If title is not provided, defaults to today's date.
     */
    @Transactional
    fun createEntry(userId: UUID, request: CreateJournalEntryRequest): JournalEntryResponse {
        val user = userRepository.getReferenceById(userId)
        val entryDate = request.entryDate ?: LocalDate.now()
        
        val title = request.title?.trim()?.takeIf { it.isNotBlank() }
            ?: entryDate.toString()

        val entry = JournalEntry(
            user = user,
            title = title,
            body = request.body?.trim() ?: "",
            entryDate = entryDate
        )

        val savedEntry = journalEntryRepository.save(entry)
        
        // Queue async mood analysis if body has content
        if (savedEntry.body.isNotBlank()) {
            moodAnalysisService.queueMoodAnalysis(savedEntry.id, savedEntry.body)
        }
        
        return savedEntry.toResponse()
    }

    /**
     * Get a journal entry by ID.
     * Verifies the entry belongs to the requesting user.
     */
    @Transactional(readOnly = true)
    fun getEntry(userId: UUID, entryId: UUID): JournalEntryResponse {
        val entry = findEntryForUser(userId, entryId)
        return entry.toResponse()
    }

    /**
     * Get paginated list of journal entries for a user.
     */
    @Transactional(readOnly = true)
    fun getEntries(userId: UUID, page: Int, size: Int): JournalEntriesPageResponse {
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "entryDate"))
        val entriesPage = journalEntryRepository.findByUserIdOrderByEntryDateDesc(userId, pageable)

        return JournalEntriesPageResponse(
            entries = entriesPage.content.map { it.toSummaryResponse() },
            page = entriesPage.number,
            size = entriesPage.size,
            totalElements = entriesPage.totalElements,
            totalPages = entriesPage.totalPages,
            hasNext = entriesPage.hasNext(),
            hasPrevious = entriesPage.hasPrevious()
        )
    }

    /**
     * Get journal entries within a date range.
     */
    @Transactional(readOnly = true)
    fun getEntriesByDateRange(
        userId: UUID, 
        startDate: LocalDate, 
        endDate: LocalDate
    ): List<JournalEntrySummaryResponse> {
        return journalEntryRepository.findByUserIdAndEntryDateBetweenOrderByEntryDateDesc(
            userId, startDate, endDate
        ).map { it.toSummaryResponse() }
    }

    /**
     * Update journal entry body content.
     * Supports optimistic locking for Google Docs-like concurrent editing detection.
     */
    @Transactional
    fun updateBody(
        userId: UUID, 
        entryId: UUID, 
        request: UpdateJournalBodyRequest
    ): JournalBodyUpdateResponse {
        val entry = findEntryForUser(userId, entryId)
        
        // Check version for optimistic locking if provided
        request.expectedVersion?.let { expectedVersion ->
            if (entry.version != expectedVersion) {
                throw ObjectOptimisticLockingFailureException(
                    JournalEntry::class.java,
                    "Journal entry was modified by another request. " +
                    "Expected version: $expectedVersion, current version: ${entry.version}"
                )
            }
        }

        entry.body = request.body
        entry.moodScore = null  // Reset mood score - will be recalculated by AI
        val savedEntry = journalEntryRepository.saveAndFlush(entry)
        
        // Queue async mood analysis (debounced via database - multiple rapid edits won't spam the API)
        moodAnalysisService.queueMoodAnalysis(savedEntry.id, savedEntry.body)

        return JournalBodyUpdateResponse(
            id = savedEntry.id,
            body = savedEntry.body,
            version = savedEntry.version,
            updatedAt = savedEntry.updatedAt
        )
    }

    /**
     * Update journal entry metadata (title only).
     * Note: mood_score is AI-generated and cannot be set by users.
     */
    @Transactional
    fun updateMetadata(
        userId: UUID, 
        entryId: UUID, 
        request: UpdateJournalMetadataRequest
    ): JournalEntryResponse {
        val entry = findEntryForUser(userId, entryId)

        request.title?.let { entry.title = it.trim() }

        val savedEntry = journalEntryRepository.save(entry)
        return savedEntry.toResponse()
    }

    /**
     * Delete a journal entry.
     */
    @Transactional
    fun deleteEntry(userId: UUID, entryId: UUID) {
        // Verify entry exists and belongs to user
        if (!journalEntryRepository.existsByUserIdAndId(userId, entryId)) {
            throw NoteNotFoundException("Journal entry not found")
        }
        
        // Cancel any pending mood analysis for this entry
        moodAnalysisService.cancelPendingAnalysis(entryId)
        
        journalEntryRepository.deleteByUserIdAndId(userId, entryId)
    }

    /**
     * Find entry and verify ownership.
     */
    private fun findEntryForUser(userId: UUID, entryId: UUID): JournalEntry {
        return journalEntryRepository.findByUserIdAndId(userId, entryId)
            ?: throw NoteNotFoundException("Journal entry not found")
    }

    private fun JournalEntry.toResponse() = JournalEntryResponse(
        id = id,
        title = title,
        body = body,
        moodScore = moodScore,
        entryDate = entryDate,
        version = version,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    private fun JournalEntry.toSummaryResponse(): JournalEntrySummaryResponse {
        val preview = if (body.length <= BODY_PREVIEW_LENGTH) {
            body
        } else {
            body.take(BODY_PREVIEW_LENGTH).trimEnd() + "..."
        }

        return JournalEntrySummaryResponse(
            id = id,
            title = title,
            bodyPreview = preview,
            moodScore = moodScore,
            entryDate = entryDate,
            updatedAt = updatedAt
        )
    }
}
