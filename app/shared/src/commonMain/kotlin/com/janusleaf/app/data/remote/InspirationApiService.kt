package com.janusleaf.app.data.remote

import com.janusleaf.app.data.remote.dto.*
import com.janusleaf.app.domain.model.InspirationError
import com.janusleaf.app.domain.model.InspirationResult
import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.*
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.json.Json

/**
 * API service for inspiration endpoints.
 * Handles HTTP communication with the backend inspiration API.
 */
class InspirationApiService(
    private val httpClient: HttpClient,
    private val baseUrl: String = ApiConfig.DEFAULT_BASE_URL
) {
    private val json = Json { ignoreUnknownKeys = true }
    
    /**
     * Get the current inspirational quote for the authenticated user.
     */
    suspend fun getInspiration(
        accessToken: String
    ): InspirationResult<InspirationalQuoteResponseDto> = safeApiCall {
        httpClient.get("$baseUrl/api/inspiration") {
            header(HttpHeaders.Authorization, "Bearer $accessToken")
        }
    }
    
    /**
     * Safe API call wrapper with error handling.
     */
    private suspend inline fun <reified T> safeApiCall(
        crossinline apiCall: suspend () -> HttpResponse
    ): InspirationResult<T> {
        return try {
            val response = apiCall()
            handleResponse(response)
        } catch (e: Exception) {
            Napier.e("Inspiration API call failed", e, tag = "InspirationApiService")
            when {
                e.message?.contains("connect", ignoreCase = true) == true ||
                e.message?.contains("timeout", ignoreCase = true) == true ||
                e.message?.contains("network", ignoreCase = true) == true -> {
                    InspirationResult.Error(InspirationError.NetworkError)
                }
                else -> InspirationResult.Error(InspirationError.UnknownError(e.message))
            }
        }
    }
    
    /**
     * Handle HTTP response and map to InspirationResult.
     */
    private suspend inline fun <reified T> handleResponse(response: HttpResponse): InspirationResult<T> {
        return when (response.status) {
            HttpStatusCode.OK -> {
                try {
                    InspirationResult.Success(response.body<T>())
                } catch (e: Exception) {
                    Napier.e("Failed to parse inspiration response", e, tag = "InspirationApiService")
                    InspirationResult.Error(InspirationError.UnknownError("Failed to parse response"))
                }
            }
            HttpStatusCode.Unauthorized -> {
                InspirationResult.Error(InspirationError.Unauthorized)
            }
            HttpStatusCode.Forbidden -> {
                InspirationResult.Error(InspirationError.Unauthorized)
            }
            HttpStatusCode.NotFound -> {
                InspirationResult.Error(InspirationError.NotFound)
            }
            HttpStatusCode.InternalServerError,
            HttpStatusCode.BadGateway,
            HttpStatusCode.ServiceUnavailable -> {
                InspirationResult.Error(InspirationError.ServerError)
            }
            else -> {
                val errorBody = tryParseError(response)
                InspirationResult.Error(InspirationError.UnknownError(errorBody?.message ?: "Unknown error"))
            }
        }
    }
    
    private suspend fun tryParseError(response: HttpResponse): ErrorResponseDto? {
        return try {
            val body = response.bodyAsText()
            json.decodeFromString<ErrorResponseDto>(body)
        } catch (e: Exception) {
            null
        }
    }
}
