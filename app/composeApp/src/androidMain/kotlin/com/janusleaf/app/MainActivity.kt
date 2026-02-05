package com.janusleaf.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.janusleaf.app.home.HomeNavKey
import com.janusleaf.app.home.homeNavEntry
import com.janusleaf.app.ui.theme.JanusLeafTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            JanusLeafTheme {
                MainComposable()
            }
        }
    }
}

@Composable
private fun MainComposable() {
    val backStack = rememberNavBackStack(HomeNavKey)
    NavDisplay(
        backStack = backStack,
        entryProvider = entryProvider {
            homeNavEntry()
        }
    )
}
