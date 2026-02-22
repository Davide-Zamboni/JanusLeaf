package com.janusleaf.app.ui.navigation.entries

import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.janusleaf.app.ui.navigation.MoodInsightsNavKey
import com.janusleaf.app.ui.mood.MoodInsightsScreen
import com.janusleaf.app.presentation.viewmodel.MoodInsightsViewModel

fun EntryProviderScope<NavKey>.moodInsightsEntry(
    onProfileClick: () -> Unit,
    onNavigateToJournal: () -> Unit,
    onNavigateToInsights: () -> Unit
) {
    entry<MoodInsightsNavKey> {
        val viewModel: MoodInsightsViewModel = rememberKmpViewModel()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        MoodInsightsScreen(
            uiState = uiState,
            loadEntries = viewModel::loadEntries,
            onProfileClick = onProfileClick,
            onNavigateToJournal = onNavigateToJournal,
            onNavigateToInsights = onNavigateToInsights
        )
    }
}
