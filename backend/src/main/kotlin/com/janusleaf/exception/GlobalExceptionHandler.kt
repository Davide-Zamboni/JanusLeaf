package com.janusleaf.exception

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.context.request.WebRequest
import java.time.Instant

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationExceptions(
        ex: MethodArgumentNotValidException,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        val errors = ex.bindingResult.fieldErrors.associate { 
            it.field to (it.defaultMessage ?: "Invalid value")
        }
        
        val errorResponse = ErrorResponse(
            error = "VALIDATION_ERROR",
            message = "Validation failed",
            details = errors,
            timestamp = Instant.now(),
            path = request.getDescription(false).removePrefix("uri=")
        )
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse)
    }

    @ExceptionHandler(UserAlreadyExistsException::class)
    fun handleUserAlreadyExists(
        ex: UserAlreadyExistsException,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        return buildErrorResponse(
            error = "USER_ALREADY_EXISTS",
            message = ex.message ?: "User already exists",
            status = HttpStatus.CONFLICT,
            request = request
        )
    }

    @ExceptionHandler(InvalidCredentialsException::class)
    fun handleInvalidCredentials(
        ex: InvalidCredentialsException,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        return buildErrorResponse(
            error = "INVALID_CREDENTIALS",
            message = ex.message ?: "Invalid credentials",
            status = HttpStatus.UNAUTHORIZED,
            request = request
        )
    }

    @ExceptionHandler(InvalidTokenException::class)
    fun handleInvalidToken(
        ex: InvalidTokenException,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        return buildErrorResponse(
            error = "INVALID_TOKEN",
            message = ex.message ?: "Invalid token",
            status = HttpStatus.UNAUTHORIZED,
            request = request
        )
    }

    @ExceptionHandler(UserNotFoundException::class)
    fun handleUserNotFound(
        ex: UserNotFoundException,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        return buildErrorResponse(
            error = "USER_NOT_FOUND",
            message = ex.message ?: "User not found",
            status = HttpStatus.NOT_FOUND,
            request = request
        )
    }

    @ExceptionHandler(NoteNotFoundException::class)
    fun handleNoteNotFound(
        ex: NoteNotFoundException,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        return buildErrorResponse(
            error = "NOTE_NOT_FOUND",
            message = ex.message ?: "Note not found",
            status = HttpStatus.NOT_FOUND,
            request = request
        )
    }

    @ExceptionHandler(NoteAccessDeniedException::class)
    fun handleNoteAccessDenied(
        ex: NoteAccessDeniedException,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        return buildErrorResponse(
            error = "ACCESS_DENIED",
            message = ex.message ?: "Access denied",
            status = HttpStatus.FORBIDDEN,
            request = request
        )
    }

    @ExceptionHandler(AIServiceException::class)
    fun handleAIServiceException(
        ex: AIServiceException,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        return buildErrorResponse(
            error = "AI_SERVICE_ERROR",
            message = ex.message ?: "AI service unavailable",
            status = HttpStatus.SERVICE_UNAVAILABLE,
            request = request
        )
    }

    @ExceptionHandler(Exception::class)
    fun handleAllUncaughtException(
        ex: Exception,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        return buildErrorResponse(
            error = "INTERNAL_ERROR",
            message = "An unexpected error occurred",
            status = HttpStatus.INTERNAL_SERVER_ERROR,
            request = request
        )
    }

    private fun buildErrorResponse(
        error: String,
        message: String,
        status: HttpStatus,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        val errorResponse = ErrorResponse(
            error = error,
            message = message,
            timestamp = Instant.now(),
            path = request.getDescription(false).removePrefix("uri=")
        )
        return ResponseEntity.status(status).body(errorResponse)
    }
}

data class ErrorResponse(
    val error: String,
    val message: String,
    val details: Map<String, String>? = null,
    val timestamp: Instant,
    val path: String
)
