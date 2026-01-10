package com.janusleaf.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.janusleaf.config.TestContainersConfig
import com.janusleaf.dto.*
import com.janusleaf.repository.UserRepository
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotBeEmpty
import org.junit.jupiter.api.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.testcontainers.junit.jupiter.Testcontainers

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
@Import(TestContainersConfig::class)
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
@DisplayName("AuthController Integration Tests")
class AuthControllerIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var userRepository: UserRepository

    companion object {
        private var accessToken: String? = null
        private var refreshToken: String? = null
        
        private const val TEST_EMAIL = "integration@example.com"
        private const val TEST_USERNAME = "IntegrationUser"
        private const val TEST_PASSWORD = "SecurePass123!"
    }

    @BeforeEach
    fun setUp() {
        // Clean up before each test if needed
    }

    @Nested
    @DisplayName("POST /api/auth/register")
    @TestMethodOrder(MethodOrderer.OrderAnnotation::class)
    inner class Register {

        @Test
        @Order(1)
        fun `should register new user successfully`() {
            val request = RegisterRequest(
                email = TEST_EMAIL,
                username = TEST_USERNAME,
                password = TEST_PASSWORD
            )

            val result = mockMvc.perform(
                post("/api/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isCreated)
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.user.email").value(TEST_EMAIL))
                .andExpect(jsonPath("$.user.username").value(TEST_USERNAME))
                .andReturn()

            // Store tokens for later tests
            val response = objectMapper.readValue(result.response.contentAsString, AuthResponse::class.java)
            accessToken = response.accessToken
            refreshToken = response.refreshToken
            
            accessToken.shouldNotBeEmpty()
            refreshToken.shouldNotBeEmpty()
        }

        @Test
        @Order(2)
        fun `should return 409 when email already exists`() {
            val request = RegisterRequest(
                email = TEST_EMAIL,
                username = "AnotherUser",
                password = TEST_PASSWORD
            )

            mockMvc.perform(
                post("/api/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isConflict)
                .andExpect(jsonPath("$.error").value("USER_ALREADY_EXISTS"))
                .andExpect(jsonPath("$.message").value("Email '$TEST_EMAIL' is already registered"))
        }

        @Test
        fun `should return 400 for invalid email format`() {
            val request = mapOf(
                "email" to "invalid-email",
                "username" to "User",
                "password" to TEST_PASSWORD
            )

            mockMvc.perform(
                post("/api/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.details.email").value("Invalid email format"))
        }

        @Test
        fun `should return 400 for short password`() {
            val request = mapOf(
                "email" to "valid@example.com",
                "username" to "User",
                "password" to "short"
            )

            mockMvc.perform(
                post("/api/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.details.password").value("Password must be at least 8 characters"))
        }

        @Test
        fun `should return 400 for short username`() {
            val request = mapOf(
                "email" to "valid2@example.com",
                "username" to "a",
                "password" to TEST_PASSWORD
            )

            mockMvc.perform(
                post("/api/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.details.username").value("Username must be between 2 and 50 characters"))
        }

        @Test
        fun `should normalize email to lowercase`() {
            val request = RegisterRequest(
                email = "UPPERCASE@EXAMPLE.COM",
                username = "UppercaseUser",
                password = TEST_PASSWORD
            )

            mockMvc.perform(
                post("/api/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isCreated)
                .andExpect(jsonPath("$.user.email").value("uppercase@example.com"))
        }
    }

    @Nested
    @DisplayName("POST /api/auth/login")
    @TestMethodOrder(MethodOrderer.OrderAnnotation::class)
    inner class Login {

        @BeforeEach
        fun ensureUserExists() {
            // Ensure the test user exists
            if (!userRepository.existsByEmail(TEST_EMAIL)) {
                val request = RegisterRequest(
                    email = TEST_EMAIL,
                    username = TEST_USERNAME,
                    password = TEST_PASSWORD
                )
                mockMvc.perform(
                    post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                )
            }
        }

        @Test
        fun `should login successfully with valid credentials`() {
            val request = LoginRequest(
                email = TEST_EMAIL,
                password = TEST_PASSWORD
            )

            val result = mockMvc.perform(
                post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.user.email").value(TEST_EMAIL))
                .andReturn()

            val response = objectMapper.readValue(result.response.contentAsString, AuthResponse::class.java)
            accessToken = response.accessToken
            refreshToken = response.refreshToken
        }

        @Test
        fun `should return 401 for wrong password`() {
            val request = LoginRequest(
                email = TEST_EMAIL,
                password = "wrongPassword123!"
            )

            mockMvc.perform(
                post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isUnauthorized)
                .andExpect(jsonPath("$.error").value("INVALID_CREDENTIALS"))
        }

        @Test
        fun `should return 401 for non-existent user`() {
            val request = LoginRequest(
                email = "nonexistent@example.com",
                password = TEST_PASSWORD
            )

            mockMvc.perform(
                post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isUnauthorized)
                .andExpect(jsonPath("$.error").value("INVALID_CREDENTIALS"))
        }

        @Test
        fun `should normalize email to lowercase for login`() {
            val request = LoginRequest(
                email = TEST_EMAIL.uppercase(),
                password = TEST_PASSWORD
            )

            mockMvc.perform(
                post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.user.email").value(TEST_EMAIL))
        }
    }

    @Nested
    @DisplayName("POST /api/auth/refresh")
    inner class RefreshToken {

        @BeforeEach
        fun ensureTokensExist() {
            if (refreshToken == null) {
                // Login to get tokens
                val loginRequest = LoginRequest(email = TEST_EMAIL, password = TEST_PASSWORD)
                
                // First ensure user exists
                if (!userRepository.existsByEmail(TEST_EMAIL)) {
                    val registerRequest = RegisterRequest(
                        email = TEST_EMAIL,
                        username = TEST_USERNAME,
                        password = TEST_PASSWORD
                    )
                    mockMvc.perform(
                        post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(registerRequest))
                    )
                }

                val result = mockMvc.perform(
                    post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest))
                ).andReturn()

                val response = objectMapper.readValue(result.response.contentAsString, AuthResponse::class.java)
                accessToken = response.accessToken
                refreshToken = response.refreshToken
            }
        }

        @Test
        fun `should refresh token successfully`() {
            val request = RefreshTokenRequest(refreshToken = refreshToken!!)

            mockMvc.perform(
                post("/api/auth/refresh")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").exists())
        }

        @Test
        fun `should return 401 for invalid refresh token`() {
            val request = RefreshTokenRequest(refreshToken = "invalid.token.here")

            mockMvc.perform(
                post("/api/auth/refresh")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isUnauthorized)
                .andExpect(jsonPath("$.error").value("INVALID_TOKEN"))
        }

        @Test
        fun `should return 401 when using access token as refresh token`() {
            val request = RefreshTokenRequest(refreshToken = accessToken!!)

            mockMvc.perform(
                post("/api/auth/refresh")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isUnauthorized)
                .andExpect(jsonPath("$.error").value("INVALID_TOKEN"))
        }
    }

    @Nested
    @DisplayName("GET /api/auth/me")
    inner class GetCurrentUser {

        @BeforeEach
        fun ensureTokenExists() {
            if (accessToken == null) {
                if (!userRepository.existsByEmail(TEST_EMAIL)) {
                    val registerRequest = RegisterRequest(
                        email = TEST_EMAIL,
                        username = TEST_USERNAME,
                        password = TEST_PASSWORD
                    )
                    mockMvc.perform(
                        post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(registerRequest))
                    )
                }

                val loginRequest = LoginRequest(email = TEST_EMAIL, password = TEST_PASSWORD)
                val result = mockMvc.perform(
                    post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest))
                ).andReturn()

                val response = objectMapper.readValue(result.response.contentAsString, AuthResponse::class.java)
                accessToken = response.accessToken
                refreshToken = response.refreshToken
            }
        }

        @Test
        fun `should return current user when authenticated`() {
            mockMvc.perform(
                get("/api/auth/me")
                    .header("Authorization", "Bearer $accessToken")
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.email").value(TEST_EMAIL))
                .andExpect(jsonPath("$.username").exists())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.createdAt").exists())
        }

        @Test
        fun `should return 403 without authentication`() {
            mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isForbidden)
        }

        @Test
        fun `should return 403 with invalid token`() {
            mockMvc.perform(
                get("/api/auth/me")
                    .header("Authorization", "Bearer invalid.token.here")
            )
                .andExpect(status().isForbidden)
        }
    }

    @Nested
    @DisplayName("PUT /api/auth/me")
    inner class UpdateProfile {

        @BeforeEach
        fun ensureTokenExists() {
            if (accessToken == null) {
                if (!userRepository.existsByEmail(TEST_EMAIL)) {
                    val registerRequest = RegisterRequest(
                        email = TEST_EMAIL,
                        username = TEST_USERNAME,
                        password = TEST_PASSWORD
                    )
                    mockMvc.perform(
                        post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(registerRequest))
                    )
                }

                val loginRequest = LoginRequest(email = TEST_EMAIL, password = TEST_PASSWORD)
                val result = mockMvc.perform(
                    post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest))
                ).andReturn()

                val response = objectMapper.readValue(result.response.contentAsString, AuthResponse::class.java)
                accessToken = response.accessToken
            }
        }

        @Test
        fun `should update username successfully`() {
            val request = UpdateProfileRequest(username = "UpdatedUsername")

            mockMvc.perform(
                put("/api/auth/me")
                    .header("Authorization", "Bearer $accessToken")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.username").value("UpdatedUsername"))
                .andExpect(jsonPath("$.email").value(TEST_EMAIL))
        }

        @Test
        fun `should return 403 without authentication`() {
            val request = UpdateProfileRequest(username = "NewName")

            mockMvc.perform(
                put("/api/auth/me")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isForbidden)
        }
    }

    @Nested
    @DisplayName("POST /api/auth/change-password")
    inner class ChangePassword {

        private val changePasswordEmail = "changepassword@example.com"
        private var changePasswordToken: String? = null

        @BeforeEach
        fun setup() {
            if (!userRepository.existsByEmail(changePasswordEmail)) {
                val registerRequest = RegisterRequest(
                    email = changePasswordEmail,
                    username = "ChangePasswordUser",
                    password = TEST_PASSWORD
                )
                mockMvc.perform(
                    post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest))
                )
            }

            val loginRequest = LoginRequest(email = changePasswordEmail, password = TEST_PASSWORD)
            val result = mockMvc.perform(
                post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(loginRequest))
            ).andReturn()

            val response = objectMapper.readValue(result.response.contentAsString, AuthResponse::class.java)
            changePasswordToken = response.accessToken
        }

        @Test
        fun `should change password successfully`() {
            val request = ChangePasswordRequest(
                currentPassword = TEST_PASSWORD,
                newPassword = "NewSecurePass456!"
            )

            mockMvc.perform(
                post("/api/auth/change-password")
                    .header("Authorization", "Bearer $changePasswordToken")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.message").value("Password changed successfully"))

            // Verify can login with new password
            val loginRequest = LoginRequest(
                email = changePasswordEmail,
                password = "NewSecurePass456!"
            )
            mockMvc.perform(
                post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(loginRequest))
            )
                .andExpect(status().isOk)
        }

        @Test
        fun `should return 401 for wrong current password`() {
            val request = ChangePasswordRequest(
                currentPassword = "wrongCurrentPassword",
                newPassword = "NewSecurePass456!"
            )

            mockMvc.perform(
                post("/api/auth/change-password")
                    .header("Authorization", "Bearer $changePasswordToken")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isUnauthorized)
                .andExpect(jsonPath("$.error").value("INVALID_CREDENTIALS"))
        }
    }

    @Nested
    @DisplayName("GET /api/health")
    inner class Health {

        @Test
        fun `should return health status without authentication`() {
            mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.version").value("1.0.0"))
                .andExpect(jsonPath("$.timestamp").exists())
        }
    }
}
