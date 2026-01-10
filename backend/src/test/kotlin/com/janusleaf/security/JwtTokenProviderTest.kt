package com.janusleaf.security

import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.util.*

@DisplayName("JwtTokenProvider")
class JwtTokenProviderTest {

    private lateinit var jwtTokenProvider: JwtTokenProvider

    private val testUserId = UUID.randomUUID()
    private val testEmail = "test@example.com"
    
    // Use a 256-bit secret for HS384
    private val testSecret = "this-is-a-test-secret-key-that-is-at-least-256-bits-long-for-testing"
    private val accessTokenExpiration = 3600000L  // 1 hour
    private val refreshTokenExpiration = 86400000L // 24 hours

    @BeforeEach
    fun setUp() {
        jwtTokenProvider = JwtTokenProvider(
            jwtSecret = testSecret,
            accessTokenExpiration = accessTokenExpiration,
            refreshTokenExpiration = refreshTokenExpiration
        )
    }

    @Nested
    @DisplayName("generateAccessToken()")
    inner class GenerateAccessToken {

        @Test
        fun `should generate valid access token`() {
            // When
            val token = jwtTokenProvider.generateAccessToken(testUserId, testEmail)

            // Then
            token shouldNotBe null
            token.split(".").size shouldBe 3 // JWT has 3 parts
        }

        @Test
        fun `should include correct claims in access token`() {
            // When
            val token = jwtTokenProvider.generateAccessToken(testUserId, testEmail)

            // Then
            jwtTokenProvider.getUserIdFromToken(token) shouldBe testUserId
            jwtTokenProvider.getEmailFromToken(token) shouldBe testEmail
            jwtTokenProvider.getTokenType(token) shouldBe JwtTokenProvider.TokenType.ACCESS
        }
    }

    @Nested
    @DisplayName("generateRefreshToken()")
    inner class GenerateRefreshToken {

        @Test
        fun `should generate valid refresh token`() {
            // When
            val token = jwtTokenProvider.generateRefreshToken(testUserId, testEmail)

            // Then
            token shouldNotBe null
            token.split(".").size shouldBe 3
        }

        @Test
        fun `should include REFRESH type in refresh token`() {
            // When
            val token = jwtTokenProvider.generateRefreshToken(testUserId, testEmail)

            // Then
            jwtTokenProvider.getTokenType(token) shouldBe JwtTokenProvider.TokenType.REFRESH
        }

        @Test
        fun `should generate different tokens for access and refresh`() {
            // When
            val accessToken = jwtTokenProvider.generateAccessToken(testUserId, testEmail)
            val refreshToken = jwtTokenProvider.generateRefreshToken(testUserId, testEmail)

            // Then
            accessToken shouldNotBe refreshToken
        }
    }

    @Nested
    @DisplayName("validateToken()")
    inner class ValidateToken {

        @Test
        fun `should return true for valid token`() {
            // Given
            val token = jwtTokenProvider.generateAccessToken(testUserId, testEmail)

            // When
            val isValid = jwtTokenProvider.validateToken(token)

            // Then
            isValid.shouldBeTrue()
        }

        @Test
        fun `should return false for malformed token`() {
            // Given
            val malformedToken = "not.a.valid.jwt.token"

            // When
            val isValid = jwtTokenProvider.validateToken(malformedToken)

            // Then
            isValid.shouldBeFalse()
        }

        @Test
        fun `should return false for empty token`() {
            // When
            val isValid = jwtTokenProvider.validateToken("")

            // Then
            isValid.shouldBeFalse()
        }

        @Test
        fun `should return false for token signed with different secret`() {
            // Given - Create provider with different secret
            val otherProvider = JwtTokenProvider(
                jwtSecret = "different-secret-key-that-is-also-at-least-256-bits-long-for-test",
                accessTokenExpiration = accessTokenExpiration,
                refreshTokenExpiration = refreshTokenExpiration
            )
            val tokenFromOtherProvider = otherProvider.generateAccessToken(testUserId, testEmail)

            // When
            val isValid = jwtTokenProvider.validateToken(tokenFromOtherProvider)

            // Then
            isValid.shouldBeFalse()
        }

        @Test
        fun `should return false for expired token`() {
            // Given - Create provider with very short expiration
            val shortExpirationProvider = JwtTokenProvider(
                jwtSecret = testSecret,
                accessTokenExpiration = 1L, // 1ms
                refreshTokenExpiration = 1L
            )
            val expiredToken = shortExpirationProvider.generateAccessToken(testUserId, testEmail)
            
            // Wait for token to expire
            Thread.sleep(10)

            // When
            val isValid = jwtTokenProvider.validateToken(expiredToken)

            // Then
            isValid.shouldBeFalse()
        }
    }

    @Nested
    @DisplayName("getUserIdFromToken()")
    inner class GetUserIdFromToken {

        @Test
        fun `should extract correct user ID from token`() {
            // Given
            val token = jwtTokenProvider.generateAccessToken(testUserId, testEmail)

            // When
            val extractedUserId = jwtTokenProvider.getUserIdFromToken(token)

            // Then
            extractedUserId shouldBe testUserId
        }
    }

    @Nested
    @DisplayName("getEmailFromToken()")
    inner class GetEmailFromToken {

        @Test
        fun `should extract correct email from token`() {
            // Given
            val token = jwtTokenProvider.generateAccessToken(testUserId, testEmail)

            // When
            val extractedEmail = jwtTokenProvider.getEmailFromToken(token)

            // Then
            extractedEmail shouldBe testEmail
        }
    }

    @Nested
    @DisplayName("getExpirationFromToken()")
    inner class GetExpirationFromToken {

        @Test
        fun `should return future expiration date for valid token`() {
            // Given
            val token = jwtTokenProvider.generateAccessToken(testUserId, testEmail)

            // When
            val expiration = jwtTokenProvider.getExpirationFromToken(token)

            // Then
            expiration.time > System.currentTimeMillis()
        }
    }

    @Nested
    @DisplayName("getAccessTokenExpirationMs()")
    inner class GetAccessTokenExpirationMs {

        @Test
        fun `should return configured expiration time`() {
            // When
            val expiration = jwtTokenProvider.getAccessTokenExpirationMs()

            // Then
            expiration shouldBe accessTokenExpiration
        }
    }
}
