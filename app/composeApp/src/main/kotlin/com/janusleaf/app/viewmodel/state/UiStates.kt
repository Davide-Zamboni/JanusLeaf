package com.janusleaf.app.viewmodel.state

import com.janusleaf.app.domain.model.InspirationalQuote
import com.janusleaf.app.domain.model.Journal
import com.janusleaf.app.domain.model.JournalPreview

data class JournalListUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val entries: List<JournalPreview> = emptyList(),
    val hasMore: Boolean = true,
    val isCreatingEntry: Boolean = false
)

data class JournalEditorUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val entry: Journal? = null,
    val isSaving: Boolean = false,
    val lastSavedAtEpochMillis: Long? = null
)

data class MoodInsightsUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val entries: List<JournalPreview> = emptyList()
)

data class InspirationUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val quote: InspirationalQuote? = null,
    val isNotFound: Boolean = false
)
