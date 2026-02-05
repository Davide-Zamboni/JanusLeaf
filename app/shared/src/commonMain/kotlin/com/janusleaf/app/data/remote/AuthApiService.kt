package com.janusleaf.app.data.remote

import com.janusleaf.app.data.remote.dto.*
import com.janusleaf.app.domain.model.AuthError
import com.janusleaf.app.domain.model.AuthResult
import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.json.Json
import kotlinx.coroutines.CancellationException

/**
 * API service for authentication endpoints.
 * Handles all HTTP communication with the backend auth API.
 */
class AuthApiService(
    private val httpClient: HttpClient,
    private val baseUrl: String = ApiConfig.DEFAULT_BASE_URL
) {
    private val json = Json { ignoreUnknownKeys = true }
    
    /**
     * Register a new user.
     */
    suspend fun register(
        email: String,
        username: String,
        password: String
    ): AuthResult<AuthResponseDto> = safeApiCall {
        httpClient.post("$baseUrl${ApiConfig.Endpoints.REGISTER}") {
            setBody(RegisterRequestDto(email, username, password))
        }
    }
    
    /**
     * Login with email and password.
     */
    suspend fun login(
        email: String,
        password: String
    ): AuthResult<AuthResponseDto> = safeApiCall {
        httpClient.post("$baseUrl${ApiConfig.Endpoints.LOGIN}") {
            setBody(LoginRequestDto(email, password))
        }
    }
    
    /**
     * Refresh the access token.
     */
    suspend fun refreshToken(refreshToken: String): AuthResult<TokenRefreshResponseDto> = safeApiCall {
        httpClient.post("$baseUrl${ApiConfig.Endpoints.REFRESH}") {
            setBody(RefreshTokenRequestDto(refreshToken))
        }
    }
    
    /**
     * Logout from current device.
     */
    suspend fun logout(refreshToken: String): AuthResult<MessageResponseDto> = safeApiCall {
        httpClient.post("$baseUrl${ApiConfig.Endpoints.LOGOUT}") {
            setBody(LogoutRequestDto(refreshToken))
        }
    }
    
    /**
     * Logout from all devices (requires access token).
     */
    suspend fun logoutAll(accessToken: String): AuthResult<MessageResponseDto> = safeApiCall {
        httpClient.post("$baseUrl${ApiConfig.Endpoints.LOGOUT_ALL}") {
            header(HttpHeaders.Authorization, "Bearer $accessToken")
        }
    }
    
    /**
     * Get current user profile.
     */
    suspend fun getCurrentUser(accessToken: String): AuthResult<UserResponseDto> = safeApiCall {
        httpClient.get("$baseUrl${ApiConfig.Endpoints.ME}") {
            header(HttpHeaders.Authorization, "Bearer $accessToken")
        }
    }
    
    /**
     * Update user profile.
     */
    suspend fun updateProfile(
        accessToken: String,
        username: String
    ): AuthResult<UserResponseDto> = safeApiCall {
        httpClient.put("$baseUrl${ApiConfig.Endpoints.ME}") {
            header(HttpHeaders.Authorization, "Bearer $accessToken")
            setBody(UpdateProfileRequestDto(username))
        }
    }
    
    /**
     * Change password.
     */
    suspend fun changePassword(
        accessToken: String,
        currentPassword: String,
        newPassword: String
    ): AuthResult<MessageResponseDto> = safeApiCall {
        httpClient.post("$baseUrl${ApiConfig.Endpoints.CHANGE_PASSWORD}") {
            header(HttpHeaders.Authorization, "Bearer $accessToken")
            setBody(ChangePasswordRequestDto(currentPassword, newPassword))
        }
    }
    
    /**
     * Safe API call wrapper with error handling.
     */
    private suspend inline fun <reified T> safeApiCall(
        crossinline apiCall: suspend () -> HttpResponse
    ): AuthResult<T> {
        return try {
            val response = apiCall()
            handleResponse(response)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Napier.e("API call failed", e, tag = "AuthApiService")
            when {
                e.message?.contains("connect", ignoreCase = true) == true ||
                e.message?.contains("timeout", ignoreCase = true) == true ||
                e.message?.contains("network", ignoreCase = true) == true -> {
                    AuthResult.Error(AuthError.NetworkError)
                }
                else -> AuthResult.Error(AuthError.UnknownError(e.message))
            }
        }
    }
    
    /**
     * Handle HTTP response and map to AuthResult.
     */
    private suspend inline fun <reified T> handleResponse(response: HttpResponse): AuthResult<T> {
        return when (response.status) {
            HttpStatusCode.OK, HttpStatusCode.Created -> {
                try {
                    AuthResult.Success(response.body<T>())
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    Napier.e("Failed to parse response", e, tag = "AuthApiService")
                    AuthResult.Error(AuthError.UnknownError("Failed to parse response"))
                }
            }
            HttpStatusCode.BadRequest -> {
                val errorBody = tryParseError(response)
                AuthResult.Error(AuthError.ValidationError(errorBody?.message ?: "Invalid request"))
            }
            HttpStatusCode.Unauthorized -> {
                val errorBody = tryParseError(response)
                when {
                    errorBody?.message?.contains("expired", ignoreCase = true) == true -> {
                        AuthResult.Error(AuthError.TokenExpired)
                    }
                    errorBody?.message?.contains("invalid", ignoreCase = true) == true &&
                    errorBody.message.contains("token", ignoreCase = true) -> {
                        AuthResult.Error(AuthError.InvalidToken)
                    }
                    else -> AuthResult.Error(AuthError.InvalidCredentials)
                }
            }
            HttpStatusCode.Forbidden -> {
                AuthResult.Error(AuthError.InvalidToken)
            }
            HttpStatusCode.Conflict -> {
                AuthResult.Error(AuthError.EmailAlreadyExists)
            }
            HttpStatusCode.InternalServerError,
            HttpStatusCode.BadGateway,
            HttpStatusCode.ServiceUnavailable -> {
                AuthResult.Error(AuthError.ServerError)
            }
            else -> {
                val errorBody = tryParseError(response)
                AuthResult.Error(AuthError.UnknownError(errorBody?.message ?: "Unknown error"))
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
