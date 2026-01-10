package com.janusleaf.app

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.janusleaf.app.domain.repository.AuthRepository
import com.janusleaf.app.presentation.auth.AuthScreen
import com.janusleaf.app.presentation.home.HomeScreen
import com.janusleaf.app.presentation.theme.JanusLeafTheme
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/**
 * App navigation state.
 */
sealed class AppScreen {
    data object Auth : AppScreen()
    data object Home : AppScreen()
}

/**
 * Root composable for the JanusLeaf application.
 */
@Composable
fun App() {
    // Initialize Napier logging
    LaunchedEffect(Unit) {
        Napier.base(DebugAntilog())
    }
    
    JanusLeafTheme {
        val authRepository: AuthRepository = koinInject()
        val scope = rememberCoroutineScope()
        
        var currentScreen by remember { mutableStateOf<AppScreen>(AppScreen.Auth) }
        val isAuthenticated by authRepository.observeAuthState().collectAsState(initial = false)
        
        // Update screen based on auth state
        LaunchedEffect(isAuthenticated) {
            currentScreen = if (isAuthenticated) AppScreen.Home else AppScreen.Auth
        }
        
        AnimatedContent(
            targetState = currentScreen,
            transitionSpec = {
                val enter = fadeIn(tween(300)) + slideInHorizontally(tween(400)) { it / 3 }
                val exit = fadeOut(tween(300)) + slideOutHorizontally(tween(400)) { -it / 3 }
                enter togetherWith exit
            },
            label = "navigation"
        ) { screen ->
            when (screen) {
                is AppScreen.Auth -> {
                    AuthScreen(
                        onAuthSuccess = {
                            currentScreen = AppScreen.Home
                        }
                    )
                }
                is AppScreen.Home -> {
                    HomeScreen(
                        onLogout = {
                            scope.launch {
                                authRepository.clearAuthData()
                            }
                        }
                    )
                }
            }
        }
    }
}
