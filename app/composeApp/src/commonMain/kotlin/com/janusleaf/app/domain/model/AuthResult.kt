package com.janusleaf.app.domain.model

/**
 * Sealed class representing the result of an authentication operation.
 * Follows the Result pattern for explicit error handling.
 */
sealed class AuthResult<out T> {
    data class Success<T>(val data: T) : AuthResult<T>()
    data class Error(val error: AuthError) : AuthResult<Nothing>()
    data object Loading : AuthResult<Nothing>()
}

/**
 * Represents different authentication errors.
 */
sealed class AuthError {
    data object InvalidCredentials : AuthError()
    data object EmailAlreadyExists : AuthError()
    data object InvalidToken : AuthError()
    data object TokenExpired : AuthError()
    data object NetworkError : AuthError()
    data object ServerError : AuthError()
    data class ValidationError(val message: String) : AuthError()
    data class UnknownError(val message: String?) : AuthError()
    
    fun toUserMessage(): String = when (this) {
        is InvalidCredentials -> "Invalid email or password"
        is EmailAlreadyExists -> "An account with this email already exists"
        is InvalidToken -> "Session expired. Please log in again"
        is TokenExpired -> "Session expired. Please log in again"
        is NetworkError -> "Unable to connect. Check your internet connection"
        is ServerError -> "Something went wrong. Please try again later"
        is ValidationError -> message
        is UnknownError -> message ?: "An unexpected error occurred"
    }
}
