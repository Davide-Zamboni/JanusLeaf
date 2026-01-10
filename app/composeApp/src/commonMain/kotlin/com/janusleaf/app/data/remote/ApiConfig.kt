package com.janusleaf.app.data.remote

/**
 * API configuration for the JanusLeaf backend.
 */
object ApiConfig {
    // Platform-specific base URLs are set via expect/actual
    // Default fallback for common code
    const val DEFAULT_BASE_URL = "http://localhost:8080"
    
    // API endpoints
    object Endpoints {
        // Auth endpoints
        const val REGISTER = "/api/auth/register"
        const val LOGIN = "/api/auth/login"
        const val REFRESH = "/api/auth/refresh"
        const val LOGOUT = "/api/auth/logout"
        const val LOGOUT_ALL = "/api/auth/logout-all"
        const val ME = "/api/auth/me"
        const val CHANGE_PASSWORD = "/api/auth/change-password"
        
        // Journal endpoints
        const val JOURNAL = "/api/journal"
        const val JOURNAL_RANGE = "/api/journal/range"
        fun journalById(id: String) = "/api/journal/$id"
        fun journalBody(id: String) = "/api/journal/$id/body"
        
        // Health
        const val HEALTH = "/api/health"
    }
    
    // HTTP timeouts
    const val CONNECT_TIMEOUT_MS = 15_000L
    const val REQUEST_TIMEOUT_MS = 30_000L
    const val SOCKET_TIMEOUT_MS = 30_000L
}

/**
 * Platform-specific base URL.
 * - Android emulator: 10.0.2.2 (special IP to reach host)
 * - iOS simulator: localhost (shares host network)
 */
expect fun getPlatformBaseUrl(): String
