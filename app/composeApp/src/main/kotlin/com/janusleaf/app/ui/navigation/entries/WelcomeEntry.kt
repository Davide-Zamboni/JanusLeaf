package com.janusleaf.app.ui.navigation.entries

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.janusleaf.app.ui.navigation.WelcomeNavKey
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.janusleaf.app.ui.home.WelcomeScreen
import com.janusleaf.app.presentation.viewmodel.WelcomeViewModel

fun EntryProviderScope<NavKey>.welcomeEntry() {
    entry<WelcomeNavKey> {
        val viewModel: WelcomeViewModel = rememberKmpViewModel()
        val authState by viewModel.authState.collectAsStateWithLifecycle()
        WelcomeScreen(
            userEmail = authState.user?.email,
            onSignOut = viewModel::logout
        )
    }
}
