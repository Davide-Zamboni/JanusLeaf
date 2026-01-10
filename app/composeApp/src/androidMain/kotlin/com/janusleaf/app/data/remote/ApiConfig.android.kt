package com.janusleaf.app.data.remote

/**
 * Android emulator uses 10.0.2.2 to reach the host machine.
 */
actual fun getPlatformBaseUrl(): String = "http://10.0.2.2:8080"
