package com.janusleaf.app.data.remote

import com.janusleaf.app.data.remote.dto.*
import com.janusleaf.app.domain.model.JournalError
import com.janusleaf.app.domain.model.JournalResult
import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.*
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.Json
import kotlinx.coroutines.CancellationException

/**
 * API service for journal endpoints.
 * Handles all HTTP communication with the backend journal API.
 */
class JournalApiService(
    private val httpClient: HttpClient,
    private val baseUrl: String = ApiConfig.DEFAULT_BASE_URL
) {
    private val json = Json { ignoreUnknownKeys = true }
    
    /**
     * Create a new journal entry.
     */
    suspend fun createEntry(
        accessToken: String,
        title: String? = null,
        body: String? = null,
        entryDate: LocalDate? = null
    ): JournalResult<JournalResponseDto> = safeApiCall {
        httpClient.post("$baseUrl${ApiConfig.Endpoints.JOURNAL}") {
            header(HttpHeaders.Authorization, "Bearer $accessToken")
            setBody(CreateJournalRequestDto(title, body, entryDate))
        }
    }
    
    /**
     * Get a journal entry by ID.
     */
    suspend fun getEntry(
        accessToken: String,
        id: String
    ): JournalResult<JournalResponseDto> = safeApiCall {
        httpClient.get("$baseUrl${ApiConfig.Endpoints.journalById(id)}") {
            header(HttpHeaders.Authorization, "Bearer $accessToken")
        }
    }
    
    /**
     * Get paginated list of journal entries.
     */
    suspend fun listEntries(
        accessToken: String,
        page: Int = 0,
        size: Int = 20
    ): JournalResult<JournalListResponseDto> = safeApiCall {
        httpClient.get("$baseUrl${ApiConfig.Endpoints.JOURNAL}") {
            header(HttpHeaders.Authorization, "Bearer $accessToken")
            parameter("page", page)
            parameter("size", size)
        }
    }
    
    /**
     * Get journal entries within a date range.
     */
    suspend fun getEntriesByDateRange(
        accessToken: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): JournalResult<List<JournalPreviewResponseDto>> = safeApiCall {
        httpClient.get("$baseUrl${ApiConfig.Endpoints.JOURNAL_RANGE}") {
            header(HttpHeaders.Authorization, "Bearer $accessToken")
            parameter("startDate", startDate.toString())
            parameter("endDate", endDate.toString())
        }
    }
    
    /**
     * Update the body content of a journal entry.
     * Supports optimistic locking with expectedVersion.
     */
    suspend fun updateBody(
        accessToken: String,
        id: String,
        body: String,
        expectedVersion: Long? = null
    ): JournalResult<JournalBodyUpdateResponseDto> = safeApiCall {
        httpClient.patch("$baseUrl${ApiConfig.Endpoints.journalBody(id)}") {
            header(HttpHeaders.Authorization, "Bearer $accessToken")
            setBody(UpdateJournalBodyRequestDto(body, expectedVersion))
        }
    }
    
    /**
     * Update metadata (title, moodScore) of a journal entry.
     */
    suspend fun updateMetadata(
        accessToken: String,
        id: String,
        title: String? = null,
        moodScore: Int? = null,
        expectedVersion: Long? = null
    ): JournalResult<JournalResponseDto> = safeApiCall {
        httpClient.patch("$baseUrl${ApiConfig.Endpoints.journalById(id)}") {
            header(HttpHeaders.Authorization, "Bearer $accessToken")
            setBody(UpdateJournalMetadataRequestDto(title, moodScore, expectedVersion))
        }
    }
    
    /**
     * Delete a journal entry.
     */
    suspend fun deleteEntry(
        accessToken: String,
        id: String
    ): JournalResult<MessageResponseDto> = safeApiCall {
        httpClient.delete("$baseUrl${ApiConfig.Endpoints.journalById(id)}") {
            header(HttpHeaders.Authorization, "Bearer $accessToken")
        }
    }
    
    /**
     * Safe API call wrapper with error handling.
     */
    private suspend inline fun <reified T> safeApiCall(
        crossinline apiCall: suspend () -> HttpResponse
    ): JournalResult<T> {
        return try {
            val response = apiCall()
            handleResponse(response)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Napier.e("Journal API call failed", e, tag = "JournalApiService")
            when {
                e.message?.contains("connect", ignoreCase = true) == true ||
                e.message?.contains("timeout", ignoreCase = true) == true ||
                e.message?.contains("network", ignoreCase = true) == true -> {
                    JournalResult.Error(JournalError.NetworkError)
                }
                else -> JournalResult.Error(JournalError.UnknownError(e.message))
            }
        }
    }
    
    /**
     * Handle HTTP response and map to JournalResult.
     */
    private suspend inline fun <reified T> handleResponse(response: HttpResponse): JournalResult<T> {
        return when (response.status) {
            HttpStatusCode.OK, HttpStatusCode.Created -> {
                try {
                    JournalResult.Success(response.body<T>())
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    Napier.e("Failed to parse journal response", e, tag = "JournalApiService")
                    JournalResult.Error(JournalError.UnknownError("Failed to parse response"))
                }
            }
            HttpStatusCode.BadRequest -> {
                val errorBody = tryParseError(response)
                JournalResult.Error(JournalError.ValidationError(errorBody?.message ?: "Invalid request"))
            }
            HttpStatusCode.Unauthorized -> {
                JournalResult.Error(JournalError.Unauthorized)
            }
            HttpStatusCode.Forbidden -> {
                JournalResult.Error(JournalError.Unauthorized)
            }
            HttpStatusCode.NotFound -> {
                JournalResult.Error(JournalError.NotFound)
            }
            HttpStatusCode.Conflict -> {
                JournalResult.Error(JournalError.VersionConflict)
            }
            HttpStatusCode.InternalServerError,
            HttpStatusCode.BadGateway,
            HttpStatusCode.ServiceUnavailable -> {
                JournalResult.Error(JournalError.ServerError)
            }
            else -> {
                val errorBody = tryParseError(response)
                JournalResult.Error(JournalError.UnknownError(errorBody?.message ?: "Unknown error"))
            }
        }
    }
    
    private suspend fun tryParseError(response: HttpResponse): ErrorResponseDto? {
        return try {
            val body = response.bodyAsText()
            json.decodeFromString<ErrorResponseDto>(body)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            null
        }
    }
}
