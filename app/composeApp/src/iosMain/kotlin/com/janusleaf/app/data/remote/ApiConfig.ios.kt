package com.janusleaf.app.data.remote

/**
 * iOS platform base URL.
 * - Production: Uses ServerAvailabilityManager for failover between servers
 * - Development: localhost (simulator shares host network)
 * 
 * Note: For production with async availability check, use [getAvailableBaseUrl] instead.
 */
actual fun getPlatformBaseUrl(): String = 
    if (ApiConfig.USE_PRODUCTION) ApiConfig.getProductionBaseUrlSync() 
    else "http://localhost:8080"
