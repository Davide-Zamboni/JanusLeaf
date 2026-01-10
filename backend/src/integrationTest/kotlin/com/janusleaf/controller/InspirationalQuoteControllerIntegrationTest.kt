package com.janusleaf.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.janusleaf.dto.*
import com.janusleaf.model.InspirationalQuote
import com.janusleaf.model.User
import com.janusleaf.repository.InspirationalQuoteRepository
import com.janusleaf.repository.JournalEntryRepository
import com.janusleaf.repository.RefreshTokenRepository
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
import java.time.Instant
import java.util.*

/**
 * Integration tests for InspirationalQuoteController using H2 in-memory database.
 * These tests verify the full request/response cycle with Spring context.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("integrationTest")
@DisplayName("InspirationalQuoteController Integration Tests")
class InspirationalQuoteControllerIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var refreshTokenRepository: RefreshTokenRepository

    @Autowired
    private lateinit var journalEntryRepository: JournalEntryRepository

    @Autowired
    private lateinit var inspirationalQuoteRepository: InspirationalQuoteRepository

    companion object {
        private const val TEST_EMAIL = "inspiration@example.com"
        private const val TEST_USERNAME = "InspirationUser"
        private const val TEST_PASSWORD = "SecurePass123!"
    }

    private var accessToken: String = ""
    private var currentUser: User? = null

    private fun cleanDatabase() {
        inspirationalQuoteRepository.deleteAll()
        journalEntryRepository.deleteAll()
        refreshTokenRepository.deleteAll()
        userRepository.deleteAll()
    }

    private fun registerAndGetToken(): String {
        val request = RegisterRequest(email = TEST_EMAIL, username = TEST_USERNAME, password = TEST_PASSWORD)
        val result = mockMvc.perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        ).andReturn()
        val response = objectMapper.readValue(result.response.contentAsString, AuthResponse::class.java)
        currentUser = userRepository.findByEmail(TEST_EMAIL)
        return response.accessToken
    }

    private fun createQuoteForUser(
        user: User,
        quote: String = "Your journey shows remarkable growth.",
        tags: Array<String> = arrayOf("growth", "resilience", "reflection", "mindfulness")
    ): InspirationalQuote {
        val inspirationalQuote = InspirationalQuote(
            user = user,
            quote = quote,
            tags = tags,
            needsRegeneration = false,
            lastGeneratedAt = Instant.now()
        )
        return inspirationalQuoteRepository.save(inspirationalQuote)
    }

    @Nested
    @DisplayName("GET /api/inspiration")
    inner class GetInspirationalQuote {

        @BeforeEach
        fun setUp() {
            cleanDatabase()
            accessToken = registerAndGetToken()
        }

        @Test
        fun `should return quote when exists for user`() {
            // Create a quote for the user
            createQuoteForUser(currentUser!!)

            mockMvc.perform(
                get("/api/inspiration")
                    .header("Authorization", "Bearer $accessToken")
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.quote").value("Your journey shows remarkable growth."))
                .andExpect(jsonPath("$.tags").isArray)
                .andExpect(jsonPath("$.tags.length()").value(4))
                .andExpect(jsonPath("$.tags[0]").value("growth"))
                .andExpect(jsonPath("$.tags[1]").value("resilience"))
                .andExpect(jsonPath("$.tags[2]").value("reflection"))
                .andExpect(jsonPath("$.tags[3]").value("mindfulness"))
                .andExpect(jsonPath("$.generatedAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists())
        }

        @Test
        fun `should return 404 when no quote exists for user`() {
            mockMvc.perform(
                get("/api/inspiration")
                    .header("Authorization", "Bearer $accessToken")
            )
                .andExpect(status().isNotFound)
                .andExpect(jsonPath("$.message").value("No inspirational quote generated yet. One will be created shortly based on your journal entries."))
        }

        @Test
        fun `should return 403 without authentication`() {
            mockMvc.perform(get("/api/inspiration"))
                .andExpect(status().isForbidden)
        }

        @Test
        fun `should return different quotes for different users`() {
            // Create quote for first user
            createQuoteForUser(currentUser!!, quote = "Quote for user 1")

            // Get quote for user 1
            mockMvc.perform(
                get("/api/inspiration")
                    .header("Authorization", "Bearer $accessToken")
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.quote").value("Quote for user 1"))

            // Register second user
            val user2Request = RegisterRequest(
                email = "user2@example.com",
                username = "User2",
                password = TEST_PASSWORD
            )
            val user2Result = mockMvc.perform(
                post("/api/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(user2Request))
            ).andReturn()
            val user2Token = objectMapper.readValue(
                user2Result.response.contentAsString,
                AuthResponse::class.java
            ).accessToken

            // User 2 should not have a quote
            mockMvc.perform(
                get("/api/inspiration")
                    .header("Authorization", "Bearer $user2Token")
            )
                .andExpect(status().isNotFound)
        }
    }

    @Nested
    @DisplayName("Quote Regeneration Flag")
    inner class QuoteRegenerationFlag {

        @BeforeEach
        fun setUp() {
            cleanDatabase()
            accessToken = registerAndGetToken()
        }

        @Test
        fun `creating journal entry should mark quote for regeneration`() {
            // Create initial quote
            val quote = createQuoteForUser(currentUser!!)
            Assertions.assertFalse(quote.needsRegeneration)

            // Create a journal entry
            val request = CreateJournalEntryRequest(
                title = "New Journal",
                body = "This is my journal entry"
            )
            mockMvc.perform(
                post("/api/journal")
                    .header("Authorization", "Bearer $accessToken")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isCreated)

            // Check that quote is marked for regeneration
            val updatedQuote = inspirationalQuoteRepository.findByUserId(currentUser!!.id)
            Assertions.assertNotNull(updatedQuote)
            Assertions.assertTrue(updatedQuote!!.needsRegeneration)
        }
    }

    @Nested
    @DisplayName("User Isolation")
    inner class UserIsolation {

        @BeforeEach
        fun setUp() {
            cleanDatabase()
        }

        @Test
        fun `should not allow access to another user's quote`() {
            // User 1 registers and gets a quote
            val user1Token = registerAndGetToken()
            val user1 = currentUser!!
            createQuoteForUser(user1, quote = "User 1's personal quote")

            // User 2 registers
            val user2Request = RegisterRequest(
                email = "user2isolation@example.com",
                username = "User2Isolation",
                password = TEST_PASSWORD
            )
            val user2Result = mockMvc.perform(
                post("/api/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(user2Request))
            ).andReturn()
            val user2Token = objectMapper.readValue(
                user2Result.response.contentAsString,
                AuthResponse::class.java
            ).accessToken

            // User 1 should see their quote
            mockMvc.perform(
                get("/api/inspiration")
                    .header("Authorization", "Bearer $user1Token")
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.quote").value("User 1's personal quote"))

            // User 2 should get 404 (no quote for them)
            mockMvc.perform(
                get("/api/inspiration")
                    .header("Authorization", "Bearer $user2Token")
            )
                .andExpect(status().isNotFound)
        }
    }
}
