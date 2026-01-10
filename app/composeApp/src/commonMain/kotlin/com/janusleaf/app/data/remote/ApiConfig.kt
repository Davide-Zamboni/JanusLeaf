package com.janusleaf.app.data.remote

/**
 * API configuration for the JanusLeaf backend.
 */
object ApiConfig {
    // For development, use your local machine's IP or 10.0.2.2 for Android emulator
    // For production, this would be your actual server URL
    const val BASE_URL = "http://10.0.2.2:8080"
    
    // API endpoints
    object Endpoints {
        const val REGISTER = "/api/auth/register"
        const val LOGIN = "/api/auth/login"
        const val REFRESH = "/api/auth/refresh"
        const val LOGOUT = "/api/auth/logout"
        const val LOGOUT_ALL = "/api/auth/logout-all"
        const val ME = "/api/auth/me"
        const val CHANGE_PASSWORD = "/api/auth/change-password"
        const val HEALTH = "/api/health"
    }
    
    // HTTP timeouts
    const val CONNECT_TIMEOUT_MS = 15_000L
    const val REQUEST_TIMEOUT_MS = 30_000L
    const val SOCKET_TIMEOUT_MS = 30_000L
}
