package com.janusleaf.app.ui.navigation.entries

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
        JournalListScreen(
            viewModel = viewModel,
            onEntryClick = onEntryClick,
            onProfileClick = onProfileClick,
            onNavigateToJournal = onNavigateToJournal,
            onNavigateToInsights = onNavigateToInsights
        )
    }
}
