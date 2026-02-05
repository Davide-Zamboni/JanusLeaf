package com.janusleaf.app

import android.app.Application
import com.janusleaf.app.di.commonModule
import com.janusleaf.app.di.platformModule
import com.janusleaf.app.di.uiModule
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

/**
 * Application class for JanusLeaf Android app.
 * Initializes Koin dependency injection.
 */
class JanusLeafApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize Napier logging
        Napier.base(DebugAntilog())
        
        // Initialize Koin
        startKoin {
            androidLogger(Level.DEBUG)
            androidContext(this@JanusLeafApplication)
            modules(platformModule, commonModule, uiModule)
        }
        
        Napier.d("JanusLeaf Application initialized", tag = "App")
    }
}
