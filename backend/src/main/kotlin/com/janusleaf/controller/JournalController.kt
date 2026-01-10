package com.janusleaf.controller

import com.janusleaf.dto.*
import com.janusleaf.security.CurrentUser
import com.janusleaf.security.UserPrincipal
import com.janusleaf.service.JournalService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDate
import java.util.*

@RestController
@RequestMapping("/api/journal")
class JournalController(
    private val journalService: JournalService
) {

    /**
     * Create a new journal entry.
     * Title defaults to today's date if not provided.
     * POST /api/journal
     */
    @PostMapping
    fun createEntry(
        @CurrentUser user: UserPrincipal,
        @Valid @RequestBody request: CreateJournalEntryRequest
    ): ResponseEntity<JournalEntryResponse> {
        val response = journalService.createEntry(user.id, request)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    /**
     * Get a specific journal entry by ID.
     * GET /api/journal/{id}
     */
    @GetMapping("/{id}")
    fun getEntry(
        @CurrentUser user: UserPrincipal,
        @PathVariable id: UUID
    ): ResponseEntity<JournalEntryResponse> {
        val response = journalService.getEntry(user.id, id)
        return ResponseEntity.ok(response)
    }

    /**
     * Get paginated list of journal entries.
     * GET /api/journal?page=0&size=20
     */
    @GetMapping
    fun getEntries(
        @CurrentUser user: UserPrincipal,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<JournalEntriesPageResponse> {
        val response = journalService.getEntries(user.id, page, size.coerceIn(1, 100))
        return ResponseEntity.ok(response)
    }

    /**
     * Get journal entries within a date range.
     * GET /api/journal/range?startDate=2024-01-01&endDate=2024-01-31
     */
    @GetMapping("/range")
    fun getEntriesByDateRange(
        @CurrentUser user: UserPrincipal,
        @RequestParam startDate: LocalDate,
        @RequestParam endDate: LocalDate
    ): ResponseEntity<List<JournalEntrySummaryResponse>> {
        val response = journalService.getEntriesByDateRange(user.id, startDate, endDate)
        return ResponseEntity.ok(response)
    }

    /**
     * Update journal entry body content.
     * Supports optimistic locking for concurrent edit detection (Google Docs-like).
     * PATCH /api/journal/{id}/body
     */
    @PatchMapping("/{id}/body")
    fun updateBody(
        @CurrentUser user: UserPrincipal,
        @PathVariable id: UUID,
        @Valid @RequestBody request: UpdateJournalBodyRequest
    ): ResponseEntity<JournalBodyUpdateResponse> {
        val response = journalService.updateBody(user.id, id, request)
        return ResponseEntity.ok(response)
    }

    /**
     * Update journal entry metadata (title only).
     * Note: mood_score is AI-generated and cannot be set by users.
     * PATCH /api/journal/{id}
     */
    @PatchMapping("/{id}")
    fun updateMetadata(
        @CurrentUser user: UserPrincipal,
        @PathVariable id: UUID,
        @Valid @RequestBody request: UpdateJournalMetadataRequest
    ): ResponseEntity<JournalEntryResponse> {
        val response = journalService.updateMetadata(user.id, id, request)
        return ResponseEntity.ok(response)
    }

    /**
     * Delete a journal entry.
     * DELETE /api/journal/{id}
     */
    @DeleteMapping("/{id}")
    fun deleteEntry(
        @CurrentUser user: UserPrincipal,
        @PathVariable id: UUID
    ): ResponseEntity<MessageResponse> {
        journalService.deleteEntry(user.id, id)
        return ResponseEntity.ok(MessageResponse("Journal entry deleted successfully"))
    }
}
