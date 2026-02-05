package com.janusleaf.app.ui.journal

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import com.janusleaf.app.ui.preview.PreviewSamples
import com.janusleaf.app.ui.theme.JanusLeafTheme
import com.janusleaf.app.ui.mood.MoodInsightsScreen
import com.janusleaf.app.ui.profile.ProfileScreen
import com.janusleaf.app.ui.viewmodel.AuthViewModel
import com.janusleaf.app.ui.viewmodel.InspirationViewModel
import com.janusleaf.app.ui.viewmodel.JournalViewModel

private enum class MainTab { Journal, Insights }

@Composable
fun MainJournalScaffold(
    authViewModel: AuthViewModel,
    journalViewModel: JournalViewModel,
    inspirationViewModel: InspirationViewModel
) {
    var selectedTab by rememberSaveable { mutableStateOf(MainTab.Journal) }
    var editorEntryId by rememberSaveable { mutableStateOf<String?>(null) }
    var showProfile by rememberSaveable { mutableStateOf(false) }
    var isCreatingEntry by rememberSaveable { mutableStateOf(false) }

    BackHandler(enabled = editorEntryId != null || showProfile) {
        if (editorEntryId != null) {
            val entryId = editorEntryId
            if (entryId != null) {
                journalViewModel.forceSave(entryId) {
                    editorEntryId = null
                    journalViewModel.clearCurrentEntry()
                }
            } else {
                editorEntryId = null
                journalViewModel.clearCurrentEntry()
            }
        } else {
            showProfile = false
        }
    }

    Scaffold(
        bottomBar = {
            if (editorEntryId == null && !showProfile) {
                NavigationBar {
                    NavigationBarItem(
                        selected = selectedTab == MainTab.Journal,
                        onClick = { selectedTab = MainTab.Journal },
                        icon = { Icon(Icons.Filled.Book, contentDescription = null) },
                        label = { androidx.compose.material3.Text("Journal") }
                    )
                    NavigationBarItem(
                        selected = selectedTab == MainTab.Insights,
                        onClick = { selectedTab = MainTab.Insights },
                        icon = { Icon(Icons.Filled.Insights, contentDescription = null) },
                        label = { androidx.compose.material3.Text("Insights") }
                    )
                }
            }
        },
        floatingActionButton = {
            if (editorEntryId == null && !showProfile) {
                FloatingActionButton(
                    onClick = {
                        if (isCreatingEntry) return@FloatingActionButton
                        isCreatingEntry = true
                        journalViewModel.createEntry { entry ->
                            isCreatingEntry = false
                            editorEntryId = entry?.id
                        }
                    },
                ) {
                    if (isCreatingEntry) {
                        androidx.compose.material3.CircularProgressIndicator(
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(20.dp)
                        )
                    } else {
                        Icon(Icons.Filled.Add, contentDescription = null)
                    }
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                showProfile -> {
                    ProfileScreen(
                        authViewModel = authViewModel,
                        journalViewModel = journalViewModel,
                        onBack = { showProfile = false }
                    )
                }
                editorEntryId != null -> {
                    JournalEditorScreen(
                        entryId = editorEntryId ?: "",
                        journalViewModel = journalViewModel,
                        onBack = {
                            editorEntryId = null
                            journalViewModel.clearCurrentEntry()
                        }
                    )
                }
                selectedTab == MainTab.Journal -> {
                    JournalListScreen(
                        authViewModel = authViewModel,
                        journalViewModel = journalViewModel,
                        inspirationViewModel = inspirationViewModel,
                        onEntryClick = { editorEntryId = it },
                        onProfileClick = { showProfile = true }
                    )
                }
                else -> {
                    MoodInsightsScreen(
                        journalViewModel = journalViewModel,
                        onProfileClick = { showProfile = true }
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun MainJournalScaffoldPreview() {
    JanusLeafTheme {
        var selectedTab by rememberSaveable { mutableStateOf(MainTab.Journal) }
        Scaffold(
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(
                        selected = selectedTab == MainTab.Journal,
                        onClick = { selectedTab = MainTab.Journal },
                        icon = { Icon(Icons.Filled.Book, contentDescription = null) },
                        label = { androidx.compose.material3.Text("Journal") }
                    )
                    NavigationBarItem(
                        selected = selectedTab == MainTab.Insights,
                        onClick = { selectedTab = MainTab.Insights },
                        icon = { Icon(Icons.Filled.Insights, contentDescription = null) },
                        label = { androidx.compose.material3.Text("Insights") }
                    )
                }
            }
        ) { padding ->
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                JournalListContent(
                    authState = PreviewSamples.authStateLoggedIn(),
                    journalState = PreviewSamples.journalUiStateWithEntries(),
                    inspirationState = PreviewSamples.inspirationUiStateWithQuote(),
                    onEntryClick = {},
                    onProfileClick = {},
                    onCreateEntry = {},
                    onLoadMore = {}
                )
            }
        }
    }
}
