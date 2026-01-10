package com.janusleaf.app.data.remote

/**
 * iOS uses localhost since the simulator shares the host network.
 */
actual fun getPlatformBaseUrl(): String = "http://localhost:8080"
