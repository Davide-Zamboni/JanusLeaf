package com.janusleaf.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.janusleaf.dto.*
import com.janusleaf.repository.UserRepository
import org.junit.jupiter.api.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

/**
 * Integration tests for AuthController using H2 in-memory database.
 * These tests verify the full request/response cycle with Spring context.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("integrationTest")
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

    @Nested
    @DisplayName("POST /api/auth/register")
    @TestMethodOrder(MethodOrderer.OrderAnnotation::class)
    inner class Register {

        @BeforeEach
        fun cleanUp() {
            userRepository.deleteAll()
        }

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

            val response = objectMapper.readValue(result.response.contentAsString, AuthResponse::class.java)
            accessToken = response.accessToken
            refreshToken = response.refreshToken
        }

        @Test
        fun `should return 409 when email already exists`() {
            // First register a user
            val request = RegisterRequest(
                email = "duplicate@example.com",
                username = "User1",
                password = TEST_PASSWORD
            )
            mockMvc.perform(
                post("/api/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            ).andExpect(status().isCreated)

            // Try to register again with same email
            val duplicateRequest = RegisterRequest(
                email = "duplicate@example.com",
                username = "User2",
                password = TEST_PASSWORD
            )
            mockMvc.perform(
                post("/api/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(duplicateRequest))
            )
                .andExpect(status().isConflict)
                .andExpect(jsonPath("$.error").value("USER_ALREADY_EXISTS"))
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
    inner class Login {

        @BeforeEach
        fun createUser() {
            userRepository.deleteAll()
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
    }

    @Nested
    @DisplayName("POST /api/auth/refresh")
    inner class RefreshToken {

        private lateinit var currentRefreshToken: String

        @BeforeEach
        fun loginUser() {
            userRepository.deleteAll()
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

            val loginRequest = LoginRequest(email = TEST_EMAIL, password = TEST_PASSWORD)
            val result = mockMvc.perform(
                post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(loginRequest))
            ).andReturn()

            val response = objectMapper.readValue(result.response.contentAsString, AuthResponse::class.java)
            accessToken = response.accessToken
            currentRefreshToken = response.refreshToken
        }

        @Test
        fun `should refresh token successfully`() {
            val request = RefreshTokenRequest(refreshToken = currentRefreshToken)

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
    }

    @Nested
    @DisplayName("GET /api/auth/me")
    inner class GetCurrentUser {

        @BeforeEach
        fun loginUser() {
            userRepository.deleteAll()
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

            val loginRequest = LoginRequest(email = TEST_EMAIL, password = TEST_PASSWORD)
            val result = mockMvc.perform(
                post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(loginRequest))
            ).andReturn()

            val response = objectMapper.readValue(result.response.contentAsString, AuthResponse::class.java)
            accessToken = response.accessToken
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
        fun loginUser() {
            userRepository.deleteAll()
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

            val loginRequest = LoginRequest(email = TEST_EMAIL, password = TEST_PASSWORD)
            val result = mockMvc.perform(
                post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(loginRequest))
            ).andReturn()

            val response = objectMapper.readValue(result.response.contentAsString, AuthResponse::class.java)
            accessToken = response.accessToken
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
    }

    @Nested
    @DisplayName("POST /api/auth/change-password")
    inner class ChangePassword {

        @BeforeEach
        fun loginUser() {
            userRepository.deleteAll()
            val registerRequest = RegisterRequest(
                email = "changepwd@example.com",
                username = "ChangePwdUser",
                password = TEST_PASSWORD
            )
            mockMvc.perform(
                post("/api/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(registerRequest))
            )

            val loginRequest = LoginRequest(email = "changepwd@example.com", password = TEST_PASSWORD)
            val result = mockMvc.perform(
                post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(loginRequest))
            ).andReturn()

            val response = objectMapper.readValue(result.response.contentAsString, AuthResponse::class.java)
            accessToken = response.accessToken
        }

        @Test
        fun `should change password successfully`() {
            val request = ChangePasswordRequest(
                currentPassword = TEST_PASSWORD,
                newPassword = "NewSecurePass456!"
            )

            mockMvc.perform(
                post("/api/auth/change-password")
                    .header("Authorization", "Bearer $accessToken")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.message").value("Password changed successfully"))

            // Verify can login with new password
            val loginRequest = LoginRequest(
                email = "changepwd@example.com",
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
                    .header("Authorization", "Bearer $accessToken")
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
