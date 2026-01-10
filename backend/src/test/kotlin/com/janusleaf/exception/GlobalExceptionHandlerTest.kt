package com.janusleaf.exception

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.web.context.request.ServletWebRequest
import org.springframework.web.context.request.WebRequest

@DisplayName("GlobalExceptionHandler")
class GlobalExceptionHandlerTest {

    private lateinit var handler: GlobalExceptionHandler
    private lateinit var webRequest: WebRequest

    @BeforeEach
    fun setUp() {
        handler = GlobalExceptionHandler()
        val mockRequest = MockHttpServletRequest().apply {
            requestURI = "/api/test"
        }
        webRequest = ServletWebRequest(mockRequest)
    }

    @Nested
    @DisplayName("handleUserAlreadyExists()")
    inner class HandleUserAlreadyExists {

        @Test
        fun `should return CONFLICT status with correct error response`() {
            // Given
            val exception = UserAlreadyExistsException("Email already registered")

            // When
            val response = handler.handleUserAlreadyExists(exception, webRequest)

            // Then
            response.statusCode shouldBe HttpStatus.CONFLICT
            response.body?.error shouldBe "USER_ALREADY_EXISTS"
            response.body?.message shouldBe "Email already registered"
            response.body?.path shouldBe "/api/test"
            response.body?.timestamp shouldNotBe null
        }
    }

    @Nested
    @DisplayName("handleInvalidCredentials()")
    inner class HandleInvalidCredentials {

        @Test
        fun `should return UNAUTHORIZED status`() {
            // Given
            val exception = InvalidCredentialsException()

            // When
            val response = handler.handleInvalidCredentials(exception, webRequest)

            // Then
            response.statusCode shouldBe HttpStatus.UNAUTHORIZED
            response.body?.error shouldBe "INVALID_CREDENTIALS"
        }

        @Test
        fun `should use custom message when provided`() {
            // Given
            val exception = InvalidCredentialsException("Custom error message")

            // When
            val response = handler.handleInvalidCredentials(exception, webRequest)

            // Then
            response.body?.message shouldBe "Custom error message"
        }
    }

    @Nested
    @DisplayName("handleInvalidToken()")
    inner class HandleInvalidToken {

        @Test
        fun `should return UNAUTHORIZED status`() {
            // Given
            val exception = InvalidTokenException("Token expired")

            // When
            val response = handler.handleInvalidToken(exception, webRequest)

            // Then
            response.statusCode shouldBe HttpStatus.UNAUTHORIZED
            response.body?.error shouldBe "INVALID_TOKEN"
            response.body?.message shouldBe "Token expired"
        }
    }

    @Nested
    @DisplayName("handleUserNotFound()")
    inner class HandleUserNotFound {

        @Test
        fun `should return NOT_FOUND status`() {
            // Given
            val exception = UserNotFoundException()

            // When
            val response = handler.handleUserNotFound(exception, webRequest)

            // Then
            response.statusCode shouldBe HttpStatus.NOT_FOUND
            response.body?.error shouldBe "USER_NOT_FOUND"
        }
    }

    @Nested
    @DisplayName("handleNoteNotFound()")
    inner class HandleNoteNotFound {

        @Test
        fun `should return NOT_FOUND status`() {
            // Given
            val exception = NoteNotFoundException("Note with ID xyz not found")

            // When
            val response = handler.handleNoteNotFound(exception, webRequest)

            // Then
            response.statusCode shouldBe HttpStatus.NOT_FOUND
            response.body?.error shouldBe "NOTE_NOT_FOUND"
            response.body?.message shouldBe "Note with ID xyz not found"
        }
    }

    @Nested
    @DisplayName("handleNoteAccessDenied()")
    inner class HandleNoteAccessDenied {

        @Test
        fun `should return FORBIDDEN status`() {
            // Given
            val exception = NoteAccessDeniedException()

            // When
            val response = handler.handleNoteAccessDenied(exception, webRequest)

            // Then
            response.statusCode shouldBe HttpStatus.FORBIDDEN
            response.body?.error shouldBe "ACCESS_DENIED"
        }
    }

    @Nested
    @DisplayName("handleAIServiceException()")
    inner class HandleAIServiceException {

        @Test
        fun `should return SERVICE_UNAVAILABLE status`() {
            // Given
            val exception = AIServiceException("OpenAI rate limit exceeded")

            // When
            val response = handler.handleAIServiceException(exception, webRequest)

            // Then
            response.statusCode shouldBe HttpStatus.SERVICE_UNAVAILABLE
            response.body?.error shouldBe "AI_SERVICE_ERROR"
            response.body?.message shouldBe "OpenAI rate limit exceeded"
        }
    }

    @Nested
    @DisplayName("handleAllUncaughtException()")
    inner class HandleAllUncaughtException {

        @Test
        fun `should return INTERNAL_SERVER_ERROR for unexpected exceptions`() {
            // Given
            val exception = RuntimeException("Unexpected error")

            // When
            val response = handler.handleAllUncaughtException(exception, webRequest)

            // Then
            response.statusCode shouldBe HttpStatus.INTERNAL_SERVER_ERROR
            response.body?.error shouldBe "INTERNAL_ERROR"
            response.body?.message shouldBe "An unexpected error occurred"
        }

        @Test
        fun `should not expose internal error details`() {
            // Given
            val exception = NullPointerException("Sensitive internal error details")

            // When
            val response = handler.handleAllUncaughtException(exception, webRequest)

            // Then
            response.body?.message shouldBe "An unexpected error occurred"
        }
    }
}
