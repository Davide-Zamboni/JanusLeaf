package com.janusleaf.app.data.remote

import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * Factory for creating configured HttpClient instances.
 */
expect fun createPlatformHttpClient(): HttpClient

/**
 * Configures the HttpClient with common settings.
 */
fun HttpClient.configureForApi(): HttpClient = config {
    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            isLenient = true
            ignoreUnknownKeys = true
            coerceInputValues = true
            encodeDefaults = true
        })
    }
    
    install(Logging) {
        logger = object : Logger {
            override fun log(message: String) {
                Napier.d(message, tag = "HTTP")
            }
        }
        level = LogLevel.BODY
    }
    
    install(HttpTimeout) {
        connectTimeoutMillis = ApiConfig.CONNECT_TIMEOUT_MS
        requestTimeoutMillis = ApiConfig.REQUEST_TIMEOUT_MS
        socketTimeoutMillis = ApiConfig.SOCKET_TIMEOUT_MS
    }
    
    defaultRequest {
        contentType(ContentType.Application.Json)
        header(HttpHeaders.Accept, ContentType.Application.Json.toString())
    }
}

/**
 * Creates a fully configured HttpClient for the API.
 */
fun createApiHttpClient(): HttpClient = createPlatformHttpClient().configureForApi()
