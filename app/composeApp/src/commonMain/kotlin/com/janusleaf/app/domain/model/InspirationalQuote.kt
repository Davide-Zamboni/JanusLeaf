package com.janusleaf.app.domain.model

import kotlinx.datetime.Instant

/**
 * Domain model representing an inspirational quote generated from user's journal entries.
 */
data class InspirationalQuote(
    val id: String,
    val quote: String,
    val tags: List<String>,
    val generatedAt: Instant,
    val updatedAt: Instant
)

/**
 * Sealed class representing the result of an inspiration operation.
 */
sealed class InspirationResult<out T> {
    data class Success<T>(val data: T) : InspirationResult<T>()
    data class Error(val error: InspirationError) : InspirationResult<Nothing>()
    data object Loading : InspirationResult<Nothing>()
}

/**
 * Represents different inspiration operation errors.
 */
sealed class InspirationError {
    data object NotFound : InspirationError()
    data object Unauthorized : InspirationError()
    data object NetworkError : InspirationError()
    data object ServerError : InspirationError()
    data class UnknownError(val message: String?) : InspirationError()
    
    fun toUserMessage(): String = when (this) {
        is NotFound -> "No inspirational quote available yet"
        is Unauthorized -> "Please log in to see your inspiration"
        is NetworkError -> "Unable to connect. Check your internet connection"
        is ServerError -> "Something went wrong. Please try again later"
        is UnknownError -> message ?: "An unexpected error occurred"
    }
}
