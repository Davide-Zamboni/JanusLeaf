package com.janusleaf.app.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import java.util.concurrent.TimeUnit

actual fun createPlatformHttpClient(): HttpClient = HttpClient(OkHttp) {
    engine {
        config {
            connectTimeout(ApiConfig.CONNECT_TIMEOUT_MS, TimeUnit.MILLISECONDS)
            readTimeout(ApiConfig.SOCKET_TIMEOUT_MS, TimeUnit.MILLISECONDS)
            writeTimeout(ApiConfig.REQUEST_TIMEOUT_MS, TimeUnit.MILLISECONDS)
            retryOnConnectionFailure(true)
        }
    }
}
