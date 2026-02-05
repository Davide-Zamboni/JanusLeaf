package com.janusleaf.app.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

enum class MainTab { Journal, Insights }

@Composable
fun MainBottomBar(
    selectedTab: MainTab,
    onSelectJournal: () -> Unit,
    onSelectInsights: () -> Unit
) {
    NavigationBar {
        NavigationBarItem(
            selected = selectedTab == MainTab.Journal,
            onClick = onSelectJournal,
            icon = { Icon(Icons.Filled.Book, contentDescription = null) },
            label = { Text("Journal") }
        )
        NavigationBarItem(
            selected = selectedTab == MainTab.Insights,
            onClick = onSelectInsights,
            icon = { Icon(Icons.Filled.Insights, contentDescription = null) },
            label = { Text("Insights") }
        )
    }
}
