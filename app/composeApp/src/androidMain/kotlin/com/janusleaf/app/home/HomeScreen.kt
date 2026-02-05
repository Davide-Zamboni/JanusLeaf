package com.janusleaf.app.home

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.janusleaf.app.ui.AppRoot
import kotlinx.serialization.Serializable
@Serializable
data object HomeNavKey : NavKey

fun EntryProviderScope<NavKey>.homeNavEntry() {
    entry<HomeNavKey> {
        HomeScreen()
    }
}

@Composable
fun HomeScreen() {
    AppRoot()
}
