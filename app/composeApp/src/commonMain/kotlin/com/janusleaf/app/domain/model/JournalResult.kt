package com.janusleaf.app.domain.model

/**
 * Sealed class representing the result of a journal operation.
 * Follows the Result pattern for explicit error handling.
 */
sealed class JournalResult<out T> {
    data class Success<T>(val data: T) : JournalResult<T>()
    data class Error(val error: JournalError) : JournalResult<Nothing>()
    data object Loading : JournalResult<Nothing>()
}

/**
 * Represents different journal operation errors.
 */
sealed class JournalError {
    data object NotFound : JournalError()
    data object Unauthorized : JournalError()
    data object VersionConflict : JournalError()
    data object NetworkError : JournalError()
    data object ServerError : JournalError()
    data class ValidationError(val message: String) : JournalError()
    data class UnknownError(val message: String?) : JournalError()
    
    fun toUserMessage(): String = when (this) {
        is NotFound -> "Journal entry not found"
        is Unauthorized -> "Please log in to access your journal"
        is VersionConflict -> "This entry was modified elsewhere. Please refresh and try again."
        is NetworkError -> "Unable to connect. Check your internet connection"
        is ServerError -> "Something went wrong. Please try again later"
        is ValidationError -> message
        is UnknownError -> message ?: "An unexpected error occurred"
    }
}
