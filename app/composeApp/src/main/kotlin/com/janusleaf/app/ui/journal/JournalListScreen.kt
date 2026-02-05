package com.janusleaf.app.ui.journal

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.janusleaf.app.domain.model.JournalPreview
import com.janusleaf.app.ui.components.InspirationalQuoteCard
import com.janusleaf.app.ui.components.MoodBadge
import com.janusleaf.app.ui.components.MainBottomBar
import com.janusleaf.app.ui.components.MainTab
import com.janusleaf.app.ui.preview.PreviewSamples
import com.janusleaf.app.ui.theme.JanusLeafTheme
import com.janusleaf.app.ui.util.formatEntryDate
import com.janusleaf.app.ui.util.formatHeaderDate
import com.janusleaf.app.ui.util.formatRelativeTime
import com.janusleaf.app.ui.util.stripMarkdown
import com.janusleaf.app.viewmodel.state.AuthUiState
import com.janusleaf.app.viewmodel.state.InspirationUiState
import com.janusleaf.app.viewmodel.state.JournalListUiState
import com.janusleaf.app.viewmodel.JournalListViewModel
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@Composable
fun JournalListScreen(
    viewModel: JournalListViewModel,
    onEntryClick: (String) -> Unit,
    onProfileClick: () -> Unit,
    onNavigateToJournal: () -> Unit,
    onNavigateToInsights: () -> Unit
) {
    val authState by viewModel.authState.collectAsStateWithLifecycle()
    val journalState by viewModel.uiState.collectAsStateWithLifecycle()
    val inspirationState by viewModel.inspirationState.collectAsStateWithLifecycle()
    val isPreview = LocalInspectionMode.current
    LaunchedEffect(Unit) {
        if (!isPreview) {
            if (journalState.entries.isEmpty()) {
                viewModel.loadEntries()
            }
            if (inspirationState.quote == null && !inspirationState.isLoading) {
                viewModel.fetchQuote()
            }
        }
    }

    val createEntry: () -> Unit = createEntry@{
        if (journalState.isCreatingEntry) return@createEntry
        viewModel.createEntry { entry ->
            entry?.id?.let { onEntryClick(it) }
        }
    }

    Scaffold(
        bottomBar = {
            MainBottomBar(
                selectedTab = MainTab.Journal,
                onSelectJournal = onNavigateToJournal,
                onSelectInsights = onNavigateToInsights
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = createEntry) {
                if (journalState.isCreatingEntry) {
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Icon(Icons.Filled.Add, contentDescription = null)
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            JournalListContent(
                authState = authState,
                journalState = journalState,
                inspirationState = inspirationState,
                onEntryClick = onEntryClick,
                onProfileClick = onProfileClick,
                onCreateEntry = createEntry,
                onLoadMore = viewModel::loadMoreEntries
            )
        }
    }
}

@Composable
fun JournalListContent(
    authState: AuthUiState,
    journalState: JournalListUiState,
    inspirationState: InspirationUiState,
    onEntryClick: (String) -> Unit,
    onProfileClick: () -> Unit,
    onCreateEntry: () -> Unit,
    onLoadMore: () -> Unit
) {
    val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    val headerTitle = authState.user?.username?.takeIf { it.isNotBlank() }?.let { "${it}'s Journal" } ?: "Journal"

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = headerTitle, style = MaterialTheme.typography.headlineLarge)
                Text(
                    text = formatHeaderDate(today.toJavaLocalDate()),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            TextButton(onClick = onProfileClick) {
                Icon(Icons.Filled.AccountCircle, contentDescription = null)
            }
        }

        when {
            journalState.isLoading && journalState.entries.isEmpty() -> {
                LoadingState()
            }
            journalState.entries.isEmpty() -> {
                EmptyJournalState(onCreateEntry = onCreateEntry)
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        InspirationalQuoteCard(
                            quote = inspirationState.quote,
                            isLoading = inspirationState.isLoading,
                            isNotFound = inspirationState.isNotFound,
                            modifier = Modifier.padding(horizontal = 20.dp)
                        )
                    }

                    itemsIndexed(journalState.entries) { _, entry ->
                        JournalEntryCard(entry = entry, onClick = { onEntryClick(entry.id) })
                    }

                    item {
                        AnimatedVisibility(visible = journalState.hasMore) {
                            TextButton(
                                onClick = onLoadMore,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 12.dp)
                            ) {
                                Text("Load More")
                            }
                        }
                        Spacer(modifier = Modifier.height(120.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun JournalEntryCard(entry: JournalPreview, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = entry.title,
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1
                    )
                    Text(
                        text = formatEntryDate(entry.entryDate.toJavaLocalDate()),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                MoodBadge(score = entry.moodScore)
            }

            if (entry.bodyPreview.isNotBlank()) {
                Text(
                    text = stripMarkdown(entry.bodyPreview),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = formatRelativeTime(entry.updatedAt.toJavaInstant()),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.weight(1f))
                Icon(
                    imageVector = Icons.Filled.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun EmptyJournalState(onCreateEntry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "📔", style = MaterialTheme.typography.displayLarge)
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Start Your Journey",
            style = MaterialTheme.typography.headlineLarge,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Text(
            text = "Your journal is empty. Tap below to create your first entry.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(modifier = Modifier.height(20.dp))
        Button(onClick = onCreateEntry) {
            Text("Create Entry")
        }
    }
}

@Composable
private fun LoadingState() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        androidx.compose.material3.CircularProgressIndicator()
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Loading your journal...",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun kotlinx.datetime.LocalDate.toJavaLocalDate(): java.time.LocalDate {
    return java.time.LocalDate.of(this.year, this.monthNumber, this.dayOfMonth)
}

private fun kotlinx.datetime.Instant.toJavaInstant(): java.time.Instant {
    return java.time.Instant.ofEpochMilli(this.toEpochMilliseconds())
}

@Preview
@Composable
private fun JournalListScreenPreview() {
    JanusLeafTheme {
        JournalListContent(
            authState = PreviewSamples.authStateLoggedIn(),
            journalState = PreviewSamples.journalListUiStateWithEntries(),
            inspirationState = PreviewSamples.inspirationUiStateWithQuote(),
            onEntryClick = {},
            onProfileClick = {},
            onCreateEntry = {},
            onLoadMore = {}
        )
    }
}
