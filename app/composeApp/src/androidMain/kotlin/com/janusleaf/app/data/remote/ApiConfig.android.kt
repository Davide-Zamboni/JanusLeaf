package com.janusleaf.app.data.remote

/**
 * Android platform base URL.
 * - Production: Uses Render deployment URL
 * - Development: 10.0.2.2 (emulator's special IP to reach host)
 */
actual fun getPlatformBaseUrl(): String = 
    if (ApiConfig.USE_PRODUCTION) ApiConfig.PRODUCTION_BASE_URL 
    else "http://10.0.2.2:8080"
