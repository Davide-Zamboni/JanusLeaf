package com.janusleaf.app.data.remote

/**
 * iOS platform base URL.
 * - Production: Uses Render deployment URL
 * - Development: localhost (simulator shares host network)
 */
actual fun getPlatformBaseUrl(): String = 
    if (ApiConfig.USE_PRODUCTION) ApiConfig.PRODUCTION_BASE_URL 
    else "http://localhost:8080"
