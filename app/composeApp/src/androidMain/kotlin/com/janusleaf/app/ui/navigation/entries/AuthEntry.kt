package com.janusleaf.app.ui.navigation.entries

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.janusleaf.app.ui.navigation.AuthNavKey
import com.janusleaf.app.ui.auth.AuthScreen
import com.janusleaf.app.viewmodel.AuthScreenViewModel
import org.koin.androidx.compose.koinViewModel

fun EntryProviderScope<NavKey>.authEntry() {
    entry<AuthNavKey> {
        val viewModel: AuthScreenViewModel = koinViewModel()
        AuthScreen(viewModel = viewModel)
    }
}
