package com.janusleaf.app

import com.janusleaf.app.ios.IosAuthService
import com.janusleaf.app.ios.createAuthService

/**
 * Entry point for iOS.
 * Provides access to the shared Kotlin business logic.
 */
object SharedModule {
    /**
     * Create an instance of the auth service for iOS.
     */
    fun createAuthService(): IosAuthService = com.janusleaf.app.ios.createAuthService()
}
