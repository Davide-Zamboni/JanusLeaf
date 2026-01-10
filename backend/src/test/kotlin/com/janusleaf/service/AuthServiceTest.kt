package com.janusleaf.service

import com.janusleaf.dto.*
import com.janusleaf.exception.InvalidCredentialsException
import com.janusleaf.exception.InvalidTokenException
import com.janusleaf.exception.UserAlreadyExistsException
import com.janusleaf.exception.UserNotFoundException
import com.janusleaf.model.User
import com.janusleaf.repository.UserRepository
import com.janusleaf.security.JwtTokenProvider
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldNotBeEmpty
import io.mockk.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.security.crypto.password.PasswordEncoder
import java.time.Instant
import java.util.*

@DisplayName("AuthService")
class AuthServiceTest {

    private lateinit var userRepository: UserRepository
    private lateinit var passwordEncoder: PasswordEncoder
    private lateinit var jwtTokenProvider: JwtTokenProvider
    private lateinit var authService: AuthService

    private val testUser = User(
        id = UUID.randomUUID(),
        email = "test@example.com",
        username = "TestUser",
        passwordHash = "hashedPassword123",
        createdAt = Instant.now(),
        updatedAt = Instant.now()
    )

    @BeforeEach
    fun setUp() {
        userRepository = mockk()
        passwordEncoder = mockk()
        jwtTokenProvider = mockk()
        authService = AuthService(userRepository, passwordEncoder, jwtTokenProvider)
    }

    @Nested
    @DisplayName("register()")
    inner class Register {

        @Test
        fun `should register new user successfully`() {
            // Given
            val request = RegisterRequest(
                email = "newuser@example.com",
                username = "NewUser",
                password = "SecurePass123!"
            )

            every { userRepository.existsByEmail("newuser@example.com") } returns false
            every { passwordEncoder.encode("SecurePass123!") } returns "hashedPassword"
            every { userRepository.save(any()) } answers { firstArg() }
            every { jwtTokenProvider.generateAccessToken(any(), any()) } returns "access_token"
            every { jwtTokenProvider.generateRefreshToken(any(), any()) } returns "refresh_token"
            every { jwtTokenProvider.getAccessTokenExpirationMs() } returns 86400000L

            // When
            val response = authService.register(request)

            // Then
            response.accessToken shouldBe "access_token"
            response.refreshToken shouldBe "refresh_token"
            response.tokenType shouldBe "Bearer"
            response.expiresIn shouldBe 86400L
            response.user.email shouldBe "newuser@example.com"
            response.user.username shouldBe "NewUser"

            verify { userRepository.existsByEmail("newuser@example.com") }
            verify { passwordEncoder.encode("SecurePass123!") }
            verify { userRepository.save(any()) }
        }

        @Test
        fun `should normalize email to lowercase`() {
            // Given
            val request = RegisterRequest(
                email = "USER@EXAMPLE.COM",
                username = "User",
                password = "SecurePass123!"
            )

            every { userRepository.existsByEmail("user@example.com") } returns false
            every { passwordEncoder.encode(any()) } returns "hashedPassword"
            every { userRepository.save(any()) } answers { firstArg() }
            every { jwtTokenProvider.generateAccessToken(any(), any()) } returns "token"
            every { jwtTokenProvider.generateRefreshToken(any(), any()) } returns "refresh"
            every { jwtTokenProvider.getAccessTokenExpirationMs() } returns 86400000L

            // When
            val response = authService.register(request)

            // Then
            response.user.email shouldBe "user@example.com"
            verify { userRepository.existsByEmail("user@example.com") }
        }

        @Test
        fun `should throw UserAlreadyExistsException when email exists`() {
            // Given
            val request = RegisterRequest(
                email = "existing@example.com",
                username = "User",
                password = "SecurePass123!"
            )

            every { userRepository.existsByEmail("existing@example.com") } returns true

            // When/Then
            val exception = shouldThrow<UserAlreadyExistsException> {
                authService.register(request)
            }
            
            exception.message shouldBe "Email 'existing@example.com' is already registered"
            verify(exactly = 0) { userRepository.save(any()) }
        }
    }

    @Nested
    @DisplayName("login()")
    inner class Login {

        @Test
        fun `should login successfully with valid credentials`() {
            // Given
            val request = LoginRequest(
                email = "test@example.com",
                password = "correctPassword"
            )

            every { userRepository.findByEmail("test@example.com") } returns testUser
            every { passwordEncoder.matches("correctPassword", "hashedPassword123") } returns true
            every { jwtTokenProvider.generateAccessToken(testUser.id, testUser.email) } returns "access_token"
            every { jwtTokenProvider.generateRefreshToken(testUser.id, testUser.email) } returns "refresh_token"
            every { jwtTokenProvider.getAccessTokenExpirationMs() } returns 86400000L

            // When
            val response = authService.login(request)

            // Then
            response.accessToken shouldBe "access_token"
            response.refreshToken shouldBe "refresh_token"
            response.user.id shouldBe testUser.id
            response.user.email shouldBe testUser.email
        }

        @Test
        fun `should throw InvalidCredentialsException when user not found`() {
            // Given
            val request = LoginRequest(
                email = "unknown@example.com",
                password = "anyPassword"
            )

            every { userRepository.findByEmail("unknown@example.com") } returns null

            // When/Then
            shouldThrow<InvalidCredentialsException> {
                authService.login(request)
            }

            verify(exactly = 0) { passwordEncoder.matches(any(), any()) }
        }

        @Test
        fun `should throw InvalidCredentialsException when password is wrong`() {
            // Given
            val request = LoginRequest(
                email = "test@example.com",
                password = "wrongPassword"
            )

            every { userRepository.findByEmail("test@example.com") } returns testUser
            every { passwordEncoder.matches("wrongPassword", "hashedPassword123") } returns false

            // When/Then
            shouldThrow<InvalidCredentialsException> {
                authService.login(request)
            }
        }

        @Test
        fun `should normalize email to lowercase for login`() {
            // Given
            val request = LoginRequest(
                email = "TEST@EXAMPLE.COM",
                password = "correctPassword"
            )

            every { userRepository.findByEmail("test@example.com") } returns testUser
            every { passwordEncoder.matches("correctPassword", "hashedPassword123") } returns true
            every { jwtTokenProvider.generateAccessToken(any(), any()) } returns "token"
            every { jwtTokenProvider.generateRefreshToken(any(), any()) } returns "refresh"
            every { jwtTokenProvider.getAccessTokenExpirationMs() } returns 86400000L

            // When
            authService.login(request)

            // Then
            verify { userRepository.findByEmail("test@example.com") }
        }
    }

    @Nested
    @DisplayName("refreshToken()")
    inner class RefreshToken {

        @Test
        fun `should refresh token successfully`() {
            // Given
            val request = RefreshTokenRequest(refreshToken = "valid_refresh_token")
            val userId = UUID.randomUUID()

            every { jwtTokenProvider.validateToken("valid_refresh_token") } returns true
            every { jwtTokenProvider.getTokenType("valid_refresh_token") } returns JwtTokenProvider.TokenType.REFRESH
            every { jwtTokenProvider.getUserIdFromToken("valid_refresh_token") } returns userId
            every { jwtTokenProvider.getEmailFromToken("valid_refresh_token") } returns "test@example.com"
            every { userRepository.existsById(userId) } returns true
            every { jwtTokenProvider.generateAccessToken(userId, "test@example.com") } returns "new_access_token"
            every { jwtTokenProvider.getAccessTokenExpirationMs() } returns 86400000L

            // When
            val response = authService.refreshToken(request)

            // Then
            response.accessToken shouldBe "new_access_token"
            response.tokenType shouldBe "Bearer"
            response.expiresIn shouldBe 86400L
        }

        @Test
        fun `should throw InvalidTokenException when token is invalid`() {
            // Given
            val request = RefreshTokenRequest(refreshToken = "invalid_token")

            every { jwtTokenProvider.validateToken("invalid_token") } returns false

            // When/Then
            val exception = shouldThrow<InvalidTokenException> {
                authService.refreshToken(request)
            }
            
            exception.message shouldBe "Invalid or expired refresh token"
        }

        @Test
        fun `should throw InvalidTokenException when using access token instead of refresh`() {
            // Given
            val request = RefreshTokenRequest(refreshToken = "access_token_not_refresh")

            every { jwtTokenProvider.validateToken("access_token_not_refresh") } returns true
            every { jwtTokenProvider.getTokenType("access_token_not_refresh") } returns JwtTokenProvider.TokenType.ACCESS

            // When/Then
            val exception = shouldThrow<InvalidTokenException> {
                authService.refreshToken(request)
            }
            
            exception.message shouldBe "Invalid token type. Expected refresh token"
        }

        @Test
        fun `should throw InvalidTokenException when user no longer exists`() {
            // Given
            val request = RefreshTokenRequest(refreshToken = "valid_refresh_token")
            val deletedUserId = UUID.randomUUID()

            every { jwtTokenProvider.validateToken("valid_refresh_token") } returns true
            every { jwtTokenProvider.getTokenType("valid_refresh_token") } returns JwtTokenProvider.TokenType.REFRESH
            every { jwtTokenProvider.getUserIdFromToken("valid_refresh_token") } returns deletedUserId
            every { jwtTokenProvider.getEmailFromToken("valid_refresh_token") } returns "deleted@example.com"
            every { userRepository.existsById(deletedUserId) } returns false

            // When/Then
            val exception = shouldThrow<InvalidTokenException> {
                authService.refreshToken(request)
            }
            
            exception.message shouldBe "User no longer exists"
        }
    }

    @Nested
    @DisplayName("getCurrentUser()")
    inner class GetCurrentUser {

        @Test
        fun `should return user when found`() {
            // Given
            every { userRepository.findById(testUser.id) } returns Optional.of(testUser)

            // When
            val response = authService.getCurrentUser(testUser.id)

            // Then
            response.id shouldBe testUser.id
            response.email shouldBe testUser.email
            response.username shouldBe testUser.username
        }

        @Test
        fun `should throw UserNotFoundException when user not found`() {
            // Given
            val unknownId = UUID.randomUUID()
            every { userRepository.findById(unknownId) } returns Optional.empty()

            // When/Then
            shouldThrow<UserNotFoundException> {
                authService.getCurrentUser(unknownId)
            }
        }
    }

    @Nested
    @DisplayName("updateProfile()")
    inner class UpdateProfile {

        @Test
        fun `should update username successfully`() {
            // Given
            val request = UpdateProfileRequest(username = "NewUsername")
            
            every { userRepository.findById(testUser.id) } returns Optional.of(testUser)
            every { userRepository.save(any()) } answers { firstArg() }

            // When
            val response = authService.updateProfile(testUser.id, request)

            // Then
            response.username shouldBe "NewUsername"
            verify { userRepository.save(match { it.username == "NewUsername" }) }
        }

        @Test
        fun `should not update when username is null`() {
            // Given
            val request = UpdateProfileRequest(username = null)
            
            every { userRepository.findById(testUser.id) } returns Optional.of(testUser)
            every { userRepository.save(any()) } answers { firstArg() }

            // When
            val response = authService.updateProfile(testUser.id, request)

            // Then
            response.username shouldBe testUser.username
        }
    }

    @Nested
    @DisplayName("changePassword()")
    inner class ChangePassword {

        @Test
        fun `should change password successfully`() {
            // Given
            val request = ChangePasswordRequest(
                currentPassword = "currentPassword",
                newPassword = "newSecurePassword123!"
            )

            every { userRepository.findById(testUser.id) } returns Optional.of(testUser)
            every { passwordEncoder.matches("currentPassword", "hashedPassword123") } returns true
            every { passwordEncoder.encode("newSecurePassword123!") } returns "newHashedPassword"
            every { userRepository.save(any()) } answers { firstArg() }

            // When
            val response = authService.changePassword(testUser.id, request)

            // Then
            response.message shouldBe "Password changed successfully"
            verify { userRepository.save(match { it.passwordHash == "newHashedPassword" }) }
        }

        @Test
        fun `should throw InvalidCredentialsException when current password is wrong`() {
            // Given
            val request = ChangePasswordRequest(
                currentPassword = "wrongCurrentPassword",
                newPassword = "newPassword123!"
            )

            every { userRepository.findById(testUser.id) } returns Optional.of(testUser)
            every { passwordEncoder.matches("wrongCurrentPassword", "hashedPassword123") } returns false

            // When/Then
            val exception = shouldThrow<InvalidCredentialsException> {
                authService.changePassword(testUser.id, request)
            }
            
            exception.message shouldBe "Current password is incorrect"
            verify(exactly = 0) { userRepository.save(any()) }
        }
    }
}
