package com.janusleaf.app.ui.navigation.entries

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.janusleaf.app.ui.navigation.MoodInsightsNavKey
import com.janusleaf.app.ui.mood.MoodInsightsScreen
import com.janusleaf.app.viewmodel.MoodInsightsViewModel
import org.koin.androidx.compose.koinViewModel

fun EntryProviderScope<NavKey>.moodInsightsEntry(
    onProfileClick: () -> Unit,
    onNavigateToJournal: () -> Unit,
    onNavigateToInsights: () -> Unit
) {
    entry<MoodInsightsNavKey> {
        val viewModel: MoodInsightsViewModel = koinViewModel()
        MoodInsightsScreen(
            viewModel = viewModel,
            onProfileClick = onProfileClick,
            onNavigateToJournal = onNavigateToJournal,
            onNavigateToInsights = onNavigateToInsights
        )
    }
}
