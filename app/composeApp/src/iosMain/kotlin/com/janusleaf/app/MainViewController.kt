package com.janusleaf.app

import androidx.compose.ui.window.ComposeUIViewController
import com.janusleaf.app.di.commonModule
import com.janusleaf.app.di.platformModule
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier
import org.koin.core.context.startKoin
import platform.UIKit.UIViewController

/**
 * Creates the main view controller for iOS.
 */
fun MainViewController(): UIViewController {
    // Initialize Koin (only if not already initialized)
    initKoin()
    
    return ComposeUIViewController { App() }
}

private var koinInitialized = false

private fun initKoin() {
    if (!koinInitialized) {
        // Initialize Napier logging
        Napier.base(DebugAntilog())
        
        startKoin {
            modules(platformModule, commonModule)
        }
        koinInitialized = true
        Napier.d("JanusLeaf iOS initialized", tag = "App")
    }
}
