package com.janusleaf.app.ui.preview

import com.janusleaf.app.domain.model.InspirationalQuote
import com.janusleaf.app.domain.model.Journal
import com.janusleaf.app.domain.model.JournalPreview
import com.janusleaf.app.domain.model.User
import com.janusleaf.app.viewmodel.state.AuthUiState
import com.janusleaf.app.viewmodel.state.InspirationUiState
import com.janusleaf.app.viewmodel.state.JournalListUiState
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime

object PreviewSamples {
    private val nowInstant: Instant = Clock.System.now()
    private val today: LocalDate = nowInstant.toLocalDateTime(TimeZone.currentSystemDefault()).date

    fun user(): User = User(
        id = "user-1",
        email = "maya@janusleaf.app",
        username = "Maya",
        createdAt = nowInstant,
        updatedAt = nowInstant
    )

    fun journalPreviewList(): List<JournalPreview> = listOf(
        JournalPreview(
            id = "entry-1",
            title = "Morning check-in",
            bodyPreview = "Felt calm and focused after a short walk and some tea.",
            moodScore = 7,
            entryDate = today.minus(2, kotlinx.datetime.DateTimeUnit.DAY),
            updatedAt = nowInstant
        ),
        JournalPreview(
            id = "entry-2",
            title = "Evening reflection",
            bodyPreview = "Noticed some stress in the afternoon but reset with breathing.",
            moodScore = 6,
            entryDate = today.minus(1, kotlinx.datetime.DateTimeUnit.DAY),
            updatedAt = nowInstant
        )
    )

    fun journal(): Journal = Journal(
        id = "entry-1",
        title = "Morning check-in",
        body = "Today I felt steady and optimistic. I want to keep this rhythm.",
        moodScore = 7,
        entryDate = today,
        version = 1,
        createdAt = nowInstant,
        updatedAt = nowInstant
    )

    fun inspiration(): InspirationalQuote = InspirationalQuote(
        id = "quote-1",
        quote = "Small, consistent steps build the calm you seek.",
        tags = listOf("growth", "balance", "presence"),
        generatedAt = nowInstant,
        updatedAt = nowInstant
    )

    fun authStateLoggedIn(): AuthUiState = AuthUiState(
        isLoading = false,
        isAuthenticated = true,
        errorMessage = null,
        user = user()
    )

    fun authStateLoggedOut(): AuthUiState = AuthUiState(
        isLoading = false,
        isAuthenticated = false,
        errorMessage = "Invalid credentials",
        user = null
    )

    fun journalListUiStateWithEntries(): JournalListUiState = JournalListUiState(
        isLoading = false,
        errorMessage = null,
        entries = journalPreviewList(),
        hasMore = true,
        isCreatingEntry = false
    )

    fun inspirationUiStateWithQuote(): InspirationUiState = InspirationUiState(
        isLoading = false,
        errorMessage = null,
        quote = inspiration(),
        isNotFound = false
    )
}
