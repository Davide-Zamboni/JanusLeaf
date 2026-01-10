package com.janusleaf.app

import com.janusleaf.app.ios.IosAuthService
import com.janusleaf.app.ios.IosJournalService
import com.janusleaf.app.ios.createAuthService
import com.janusleaf.app.ios.createJournalService

/**
 * Entry point for iOS.
 * Provides access to the shared Kotlin business logic.
 */
object SharedModule {
    /**
     * Create an instance of the auth service for iOS.
     */
    fun createAuthService(): IosAuthService = com.janusleaf.app.ios.createAuthService()
    
    /**
     * Create an instance of the journal service for iOS.
     */
    fun createJournalService(): IosJournalService = com.janusleaf.app.ios.createJournalService()
}
