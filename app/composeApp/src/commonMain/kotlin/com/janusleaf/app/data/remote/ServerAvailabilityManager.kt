package com.janusleaf.app.data.remote

import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import kotlin.concurrent.Volatile
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Manages server availability and provides failover between multiple backend servers.
 * 
 * This manager checks server health and automatically routes to an available server.
 * It caches the last known working server to minimize health check overhead.
 */
object ServerAvailabilityManager {
    
    /**
     * Production server configuration.
     * Each entry contains the full base URL for a server.
     * The manager will try servers in this order until one responds.
     */
    private data class ServerConfig(val baseUrl: String)
    
    private val PRODUCTION_SERVERS = listOf(
        ServerConfig("http://80.225.83.90:8080"),
        ServerConfig("http://158.180.228.188:8080"),
        ServerConfig("https://janusleaf.onrender.com")
    )
    
    /**
     * Timeout for health checks (shorter than regular API calls).
     */
    private const val HEALTH_CHECK_TIMEOUT_MS = 5_000L
    
    /**
     * Cached available server URL.
     */
    @Volatile
    private var cachedServerUrl: String? = null
    
    /**
     * Timestamp of last successful health check.
     */
    @Volatile
    private var lastHealthCheckTime: Long = 0
    
    /**
     * Cache validity duration (5 minutes).
     */
    private const val CACHE_VALIDITY_MS = 5 * 60 * 1000L
    
    /**
     * Mutex to prevent concurrent health checks.
     */
    private val mutex = Mutex()
    
    /**
     * Gets the base URL for production servers with availability check.
     * 
     * This method:
     * 1. Returns cached URL if still valid
     * 2. Otherwise, checks each server in order until one responds
     * 3. Falls back to primary server if all checks fail
     * 
     * @param httpClient The HTTP client to use for health checks
     * @return The base URL of an available production server
     */
    suspend fun getAvailableProductionUrl(httpClient: HttpClient): String {
        // Check if we have a valid cached URL
        val currentTime = currentTimeMillis()
        val cached = cachedServerUrl
        if (cached != null && (currentTime - lastHealthCheckTime) < CACHE_VALIDITY_MS) {
            Napier.d("Using cached server URL: $cached", tag = "ServerAvailability")
            return cached
        }
        
        return mutex.withLock {
            // Double-check after acquiring lock
            val cachedAfterLock = cachedServerUrl
            if (cachedAfterLock != null && (currentTimeMillis() - lastHealthCheckTime) < CACHE_VALIDITY_MS) {
                return@withLock cachedAfterLock
            }
            
            // Try each server in order
            for (server in PRODUCTION_SERVERS) {
                if (isServerAvailable(httpClient, server.baseUrl)) {
                    Napier.i("Server available: ${server.baseUrl}", tag = "ServerAvailability")
                    cachedServerUrl = server.baseUrl
                    lastHealthCheckTime = currentTimeMillis()
                    return@withLock server.baseUrl
                }
            }
            
            // If no server responds, use the first one and hope for the best
            val fallbackUrl = PRODUCTION_SERVERS.first().baseUrl
            Napier.w("No server responded to health check, using fallback: $fallbackUrl", tag = "ServerAvailability")
            cachedServerUrl = fallbackUrl
            lastHealthCheckTime = currentTimeMillis()
            fallbackUrl
        }
    }
    
    /**
     * Gets the base URL for production servers synchronously (uses cached value).
     * 
     * If no cached value exists, returns the primary server URL.
     * Use [getAvailableProductionUrl] for async availability checking.
     * 
     * @return The cached or primary server URL
     */
    fun getProductionUrl(): String {
        return cachedServerUrl ?: PRODUCTION_SERVERS.first().baseUrl
    }
    
    /**
     * Checks if a server is available by hitting its health endpoint.
     * 
     * @param httpClient The HTTP client to use
     * @param serverUrl The base URL of the server to check
     * @return true if the server responds successfully
     */
    private suspend fun isServerAvailable(httpClient: HttpClient, serverUrl: String): Boolean {
        return try {
            Napier.d("Checking server availability: $serverUrl", tag = "ServerAvailability")
            
            // Create a client with shorter timeout for health checks
            val healthCheckClient = httpClient.config {
                install(HttpTimeout) {
                    connectTimeoutMillis = HEALTH_CHECK_TIMEOUT_MS
                    requestTimeoutMillis = HEALTH_CHECK_TIMEOUT_MS
                    socketTimeoutMillis = HEALTH_CHECK_TIMEOUT_MS
                }
            }
            
            val response = healthCheckClient.get("$serverUrl${ApiConfig.Endpoints.HEALTH}")
            val isAvailable = response.status == HttpStatusCode.OK
            
            if (isAvailable) {
                Napier.d("Server $serverUrl is available", tag = "ServerAvailability")
            } else {
                Napier.w("Server $serverUrl returned status: ${response.status}", tag = "ServerAvailability")
            }
            
            isAvailable
        } catch (e: Exception) {
            Napier.w("Server $serverUrl is not available: ${e.message}", tag = "ServerAvailability")
            false
        }
    }
    
    /**
     * Forces a refresh of the cached server URL on the next request.
     * Useful when a previously working server starts failing.
     */
    fun invalidateCache() {
        cachedServerUrl = null
        lastHealthCheckTime = 0
        Napier.d("Server cache invalidated", tag = "ServerAvailability")
    }
    
    /**
     * Reports a server failure, which may trigger a cache invalidation
     * if the failing server matches the cached one.
     * 
     * @param failedUrl The URL of the server that failed
     */
    fun reportServerFailure(failedUrl: String) {
        if (cachedServerUrl == failedUrl) {
            Napier.w("Cached server reported as failed, invalidating cache", tag = "ServerAvailability")
            invalidateCache()
        }
    }
}

/**
 * Platform-specific function to get current time in milliseconds.
 */
expect fun currentTimeMillis(): Long
