package com.janusleaf.app.data.remote

/**
 * Android platform base URL.
 * - Production: Uses ServerAvailabilityManager for failover between servers
 * - Development: 10.0.2.2 (emulator's special IP to reach host)
 * 
 * Note: For production with async availability check, use [getAvailableBaseUrl] instead.
 */
actual fun getPlatformBaseUrl(): String = 
    if (ApiConfig.USE_PRODUCTION) ApiConfig.getProductionBaseUrlSync() 
    else "http://10.0.2.2:8080"
