package com.janusleaf.app.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.with
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.janusleaf.app.ui.auth.AuthScreen
import com.janusleaf.app.ui.journal.MainJournalScaffold
import com.janusleaf.app.ui.viewmodel.AuthViewModel
import com.janusleaf.app.ui.viewmodel.InspirationViewModel
import com.janusleaf.app.ui.viewmodel.JournalViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun AppRoot() {
    val authViewModel: AuthViewModel = koinViewModel()
    val journalViewModel: JournalViewModel = koinViewModel()
    val inspirationViewModel: InspirationViewModel = koinViewModel()

    val authState by authViewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(authState.isAuthenticated) {
        if (!authState.isAuthenticated) {
            journalViewModel.clearAll()
            inspirationViewModel.reset()
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        AnimatedContent(
            targetState = authState.isAuthenticated,
            transitionSpec = { fadeIn() togetherWith (fadeOut()) },
            label = "auth"
        ) { isAuthenticated ->
            if (isAuthenticated) {
                MainJournalScaffold(
                    authViewModel = authViewModel,
                    journalViewModel = journalViewModel,
                    inspirationViewModel = inspirationViewModel
                )
            } else {
                AuthScreen(authViewModel = authViewModel)
            }
        }
    }
}
