package com.janusleaf.app.ui.navigation.entries

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.janusleaf.app.ui.navigation.WelcomeNavKey
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.janusleaf.app.ui.home.WelcomeScreen
import com.janusleaf.app.viewmodel.WelcomeViewModel
import org.koin.androidx.compose.koinViewModel

fun EntryProviderScope<NavKey>.welcomeEntry() {
    entry<WelcomeNavKey> {
        val viewModel: WelcomeViewModel = koinViewModel()
        val authState by viewModel.authState.collectAsStateWithLifecycle()
        WelcomeScreen(
            userEmail = authState.user?.email,
            onSignOut = viewModel::logout
        )
    }
}
