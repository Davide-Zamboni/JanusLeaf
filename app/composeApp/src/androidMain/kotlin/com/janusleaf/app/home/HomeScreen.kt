package com.janusleaf.app.home

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
@Serializable
data object HomeNavKey : NavKey

fun EntryProviderScope<NavKey>.homeNavEntry() {
    entry<HomeNavKey> {
        HomeScreen()
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HomeScreen() {
    Scaffold(containerColor = MaterialTheme.colorScheme.secondaryContainer) {
        LazyColumn(modifier = Modifier.padding(it).padding(horizontal = 16.dp)) {
            item {
                Card(modifier = Modifier.padding(16.dp)) {
                    Text(
                        modifier = Modifier.padding(16.dp),
                        text = "This is an inspirational quote."
                    )
                }
            }
            items(5) { index ->
                if (index != 0) Spacer(Modifier.size(ListItemDefaults.SegmentedGap))
                SegmentedListItem(
                    content = { Text("Journal Entry #$index") },
                    supportingContent = { Text("This is a short description of the journal entry.") },
                    leadingContent = {
                        Icon(
                            Icons.Filled.Book,
                            contentDescription = "Book icon"
                        )
                    },
                    colors = ListItemDefaults.colors(),
                    shapes = ListItemDefaults.segmentedShapes(index, 5),
                    onClick = { /* Handle click */ }
                )
            }
        }
    }
}

@Preview(device = Devices.PHONE)
@Preview(device = "spec:width=411dp,height=891dp,orientation=landscape")
@Preview(device = Devices.FOLDABLE)
@Preview(device = Devices.TABLET)
@Composable
fun HomeScreenPreview() {
    MaterialTheme {
        HomeScreen()
    }
}
