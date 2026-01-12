package com.janusleaf.app.data.remote

import com.janusleaf.app.BuildConfig
import io.ktor.client.HttpClient

/**
 * API configuration for the JanusLeaf backend.
 * 
 * Environment is determined at BUILD TIME via Gradle:
 *   Production: ./gradlew build -PuseProduction=true
 *   Development: ./gradlew build (default)
 * 
 * Production servers have automatic failover between:
 *   - Primary: 80.225.83.90:8080
 *   - Secondary: 158.180.228.188:8080
 */
object ApiConfig {
    // Use build-time configuration
    val USE_PRODUCTION: Boolean = BuildConfig.USE_PRODUCTION
    
    // Platform-specific base URLs are set via expect/actual
    // Default fallback for common code
    const val DEFAULT_BASE_URL = "http://localhost:8080"
    
    /**
     * Gets the production base URL with server availability check.
     * 
     * This method checks server health and returns the URL of an available server.
     * If both servers are unavailable, it returns the primary server URL.
     * 
     * @param httpClient The HTTP client to use for health checks
     * @return The base URL of an available production server
     */
    suspend fun getProductionBaseUrl(httpClient: HttpClient): String {
        return ServerAvailabilityManager.getAvailableProductionUrl(httpClient)
    }
    
    /**
     * Gets the production base URL synchronously (uses cached value).
     * 
     * Prefer [getProductionBaseUrl] for async availability checking.
     * This method is useful when you need the URL synchronously and
     * an availability check has already been performed.
     * 
     * @return The cached or primary production server URL
     */
    fun getProductionBaseUrlSync(): String {
        return ServerAvailabilityManager.getProductionUrl()
    }
    
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
 * 
 * In production mode, this uses the ServerAvailabilityManager
 * for automatic failover between production servers.
 */
expect fun getPlatformBaseUrl(): String

/**
 * Gets the platform base URL with async availability check for production.
 * 
 * In development mode, returns the platform-specific local URL.
 * In production mode, checks server availability and returns a working server URL.
 * 
 * @param httpClient The HTTP client to use for health checks
 * @return The base URL to use for API requests
 */
suspend fun getAvailableBaseUrl(httpClient: HttpClient): String {
    return if (ApiConfig.USE_PRODUCTION) {
        ApiConfig.getProductionBaseUrl(httpClient)
    } else {
        getPlatformBaseUrl()
    }
}
