package com.janusleaf.app.ui.navigation.entries

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.janusleaf.app.ui.navigation.JournalListNavKey
import com.janusleaf.app.ui.journal.JournalListScreen
import com.janusleaf.app.presentation.viewmodel.JournalListViewModel

fun EntryProviderScope<NavKey>.journalListEntry(
    onEntryClick: (String) -> Unit,
    onProfileClick: () -> Unit,
    onNavigateToJournal: () -> Unit,
    onNavigateToInsights: () -> Unit
) {
    entry<JournalListNavKey> {
        val viewModel: JournalListViewModel = rememberKmpViewModel()
        val authState by viewModel.authState.collectAsStateWithLifecycle()
        val journalState by viewModel.uiState.collectAsStateWithLifecycle()
        val inspirationState by viewModel.inspirationState.collectAsStateWithLifecycle()
        LaunchedEffect(journalState.pendingCreatedEntryId) {
            val entryId = journalState.pendingCreatedEntryId ?: return@LaunchedEffect
            onEntryClick(entryId)
            viewModel.consumeCreatedEntryNavigation()
        }
        JournalListScreen(
            authState = authState,
            journalState = journalState,
            inspirationState = inspirationState,
            loadEntries = viewModel::loadEntries,
            fetchQuote = viewModel::fetchQuote,
            createEntry = viewModel::createEntry,
            loadMoreEntries = viewModel::loadMoreEntries,
            onEntryClick = onEntryClick,
            onProfileClick = onProfileClick,
            onNavigateToJournal = onNavigateToJournal,
            onNavigateToInsights = onNavigateToInsights
        )
    }
}
