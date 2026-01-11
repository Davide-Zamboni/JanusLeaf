package com.janusleaf.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.janusleaf.config.OpenRouterProperties
import com.janusleaf.model.InspirationalQuote
import com.janusleaf.model.JournalEntry
import com.janusleaf.model.User
import com.janusleaf.repository.InspirationalQuoteRepository
import com.janusleaf.repository.JournalEntryRepository
import com.janusleaf.repository.UserRepository
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import java.time.Instant
import java.time.LocalDate
import java.util.*

@DisplayName("InspirationalQuoteService")
class InspirationalQuoteServiceTest {

    private lateinit var openRouterWebClient: WebClient
    private lateinit var fallbackWebClient: WebClient
    private lateinit var openRouterProperties: OpenRouterProperties
    private lateinit var inspirationalQuoteRepository: InspirationalQuoteRepository
    private lateinit var journalEntryRepository: JournalEntryRepository
    private lateinit var userRepository: UserRepository
    private lateinit var objectMapper: ObjectMapper
    private lateinit var inspirationalQuoteService: InspirationalQuoteService

    private val testUserId = UUID.randomUUID()
    private val testUser = User(
        id = testUserId,
        email = "test@example.com",
        username = "TestUser",
        passwordHash = "hashedPassword123",
        createdAt = Instant.now(),
        updatedAt = Instant.now()
    )

    private fun createTestQuote(
        id: UUID = UUID.randomUUID(),
        user: User = testUser,
        quote: String = "Test inspirational quote",
        tags: Array<String> = arrayOf("growth", "reflection", "gratitude", "mindfulness"),
        needsRegeneration: Boolean = false,
        lastGeneratedAt: Instant = Instant.now()
    ): InspirationalQuote {
        return InspirationalQuote(
            id = id,
            user = user,
            quote = quote,
            tags = tags,
            needsRegeneration = needsRegeneration,
            lastGeneratedAt = lastGeneratedAt
        )
    }

    private fun createTestEntry(
        id: UUID = UUID.randomUUID(),
        title: String = "Test Entry",
        body: String = "Test body content",
        entryDate: LocalDate = LocalDate.now()
    ): JournalEntry {
        return JournalEntry(
            id = id,
            user = testUser,
            title = title,
            body = body,
            entryDate = entryDate
        )
    }

    @BeforeEach
    fun setUp() {
        openRouterWebClient = mockk()
        fallbackWebClient = mockk()
        openRouterProperties = OpenRouterProperties(
            apiKey = "test-api-key",
            baseUrl = "https://api.test.com",
            model = "test-model",
            debounceDelayMs = 1000
        )
        inspirationalQuoteRepository = mockk()
        journalEntryRepository = mockk()
        userRepository = mockk()
        objectMapper = jacksonObjectMapper()

        inspirationalQuoteService = InspirationalQuoteService(
            openRouterWebClient,
            fallbackWebClient,
            openRouterProperties,
            inspirationalQuoteRepository,
            journalEntryRepository,
            userRepository,
            objectMapper
        )
    }

    @Nested
    @DisplayName("getQuoteForUser()")
    inner class GetQuoteForUser {

        @Test
        fun `should return quote when exists for user`() {
            // Given
            val quote = createTestQuote()
            every { inspirationalQuoteRepository.findByUserId(testUserId) } returns quote

            // When
            val result = inspirationalQuoteService.getQuoteForUser(testUserId)

            // Then
            result shouldNotBe null
            result?.quote shouldBe "Test inspirational quote"
            result?.tags shouldBe arrayOf("growth", "reflection", "gratitude", "mindfulness")
        }

        @Test
        fun `should return null when no quote exists for user`() {
            // Given
            every { inspirationalQuoteRepository.findByUserId(testUserId) } returns null

            // When
            val result = inspirationalQuoteService.getQuoteForUser(testUserId)

            // Then
            result shouldBe null
        }
    }

    @Nested
    @DisplayName("markForRegeneration()")
    inner class MarkForRegeneration {

        @Test
        fun `should mark quote for regeneration`() {
            // Given
            every { inspirationalQuoteRepository.markForRegeneration(testUserId, any()) } returns 1

            // When
            inspirationalQuoteService.markForRegeneration(testUserId)

            // Then
            verify { inspirationalQuoteRepository.markForRegeneration(testUserId, any()) }
        }

        @Test
        fun `should not fail when no quote exists to mark`() {
            // Given
            every { inspirationalQuoteRepository.markForRegeneration(testUserId, any()) } returns 0

            // When
            inspirationalQuoteService.markForRegeneration(testUserId)

            // Then
            verify { inspirationalQuoteRepository.markForRegeneration(testUserId, any()) }
        }
    }

    @Nested
    @DisplayName("processQuoteGeneration()")
    inner class ProcessQuoteGeneration {

        @Test
        fun `should skip processing when API key is blank`() {
            // Given - service with blank API key
            val propsWithNoKey = OpenRouterProperties(apiKey = "", baseUrl = "", model = "")
            val serviceNoKey = InspirationalQuoteService(
                openRouterWebClient,
                fallbackWebClient,
                propsWithNoKey,
                inspirationalQuoteRepository,
                journalEntryRepository,
                userRepository,
                objectMapper
            )

            // When
            serviceNoKey.processQuoteGeneration()

            // Then
            verify(exactly = 0) { inspirationalQuoteRepository.findUserIdsWithoutQuotes() }
            verify(exactly = 0) { inspirationalQuoteRepository.findQuotesNeedingRegeneration(any()) }
        }

        @Test
        fun `should process users without quotes`() {
            // Given
            val userWithoutQuote = UUID.randomUUID()
            val entries = listOf(createTestEntry(body = "Journal entry content"))
            val page = PageImpl(entries)
            
            every { inspirationalQuoteRepository.findUserIdsWithoutQuotes() } returns listOf(userWithoutQuote)
            every { inspirationalQuoteRepository.findQuotesNeedingRegeneration(any()) } returns emptyList()
            every { journalEntryRepository.findByUserId(userWithoutQuote, any()) } returns page
            every { userRepository.getReferenceById(userWithoutQuote) } returns testUser
            
            // Mock WebClient chain
            val requestBodyUriSpec = mockk<WebClient.RequestBodyUriSpec>()
            val requestBodySpec = mockk<WebClient.RequestBodySpec>()
            val responseSpec = mockk<WebClient.ResponseSpec>()
            
            every { openRouterWebClient.post() } returns requestBodyUriSpec
            every { requestBodyUriSpec.uri("/chat/completions") } returns requestBodySpec
            every { requestBodySpec.bodyValue(any()) } returns requestBodySpec
            every { requestBodySpec.retrieve() } returns responseSpec
            every { responseSpec.bodyToMono(OpenRouterResponse::class.java) } returns Mono.just(
                OpenRouterResponse(
                    id = "test",
                    choices = listOf(
                        Choice(
                            index = 0,
                            message = ResponseMessage(
                                role = "assistant",
                                content = """{"quote": "Stay strong!", "tags": ["strength", "courage", "hope", "growth"]}"""
                            )
                        )
                    )
                )
            )
            every { inspirationalQuoteRepository.save(any()) } answers { firstArg() }

            // When
            inspirationalQuoteService.processQuoteGeneration()

            // Then
            verify { inspirationalQuoteRepository.save(match { it.quote == "Stay strong!" }) }
        }

        @Test
        fun `should regenerate quotes that need updating`() {
            // Given
            val outdatedQuote = createTestQuote(
                needsRegeneration = true,
                lastGeneratedAt = Instant.now().minusSeconds(100000)
            )
            val entries = listOf(createTestEntry(body = "Recent journal"))
            val page = PageImpl(entries)

            every { inspirationalQuoteRepository.findUserIdsWithoutQuotes() } returns emptyList()
            every { inspirationalQuoteRepository.findQuotesNeedingRegeneration(any()) } returns listOf(outdatedQuote)
            every { journalEntryRepository.findByUserId(testUserId, any()) } returns page
            
            // Mock WebClient chain
            val requestBodyUriSpec = mockk<WebClient.RequestBodyUriSpec>()
            val requestBodySpec = mockk<WebClient.RequestBodySpec>()
            val responseSpec = mockk<WebClient.ResponseSpec>()
            
            every { openRouterWebClient.post() } returns requestBodyUriSpec
            every { requestBodyUriSpec.uri("/chat/completions") } returns requestBodySpec
            every { requestBodySpec.bodyValue(any()) } returns requestBodySpec
            every { requestBodySpec.retrieve() } returns responseSpec
            every { responseSpec.bodyToMono(OpenRouterResponse::class.java) } returns Mono.just(
                OpenRouterResponse(
                    id = "test",
                    choices = listOf(
                        Choice(
                            index = 0,
                            message = ResponseMessage(
                                role = "assistant",
                                content = """{"quote": "Keep going!", "tags": ["perseverance", "hope", "strength", "growth"]}"""
                            )
                        )
                    )
                )
            )
            every { inspirationalQuoteRepository.save(any()) } answers { firstArg() }

            // When
            inspirationalQuoteService.processQuoteGeneration()

            // Then
            verify { inspirationalQuoteRepository.save(match { 
                it.quote == "Keep going!" && 
                it.needsRegeneration == false 
            }) }
        }

        @Test
        fun `should handle API errors gracefully`() {
            // Given
            val userWithoutQuote = UUID.randomUUID()
            val entries = listOf(createTestEntry(body = "Journal content"))
            val page = PageImpl(entries)
            
            every { inspirationalQuoteRepository.findUserIdsWithoutQuotes() } returns listOf(userWithoutQuote)
            every { inspirationalQuoteRepository.findQuotesNeedingRegeneration(any()) } returns emptyList()
            every { journalEntryRepository.findByUserId(userWithoutQuote, any()) } returns page
            
            // Mock WebClient to throw exception
            val requestBodyUriSpec = mockk<WebClient.RequestBodyUriSpec>()
            val requestBodySpec = mockk<WebClient.RequestBodySpec>()
            val responseSpec = mockk<WebClient.ResponseSpec>()
            
            every { openRouterWebClient.post() } returns requestBodyUriSpec
            every { requestBodyUriSpec.uri("/chat/completions") } returns requestBodySpec
            every { requestBodySpec.bodyValue(any()) } returns requestBodySpec
            every { requestBodySpec.retrieve() } returns responseSpec
            every { responseSpec.bodyToMono(OpenRouterResponse::class.java) } returns Mono.error(RuntimeException("API Error"))

            // When - should not throw
            inspirationalQuoteService.processQuoteGeneration()

            // Then - quote should not be saved
            verify(exactly = 0) { inspirationalQuoteRepository.save(any()) }
        }
    }

    @Nested
    @DisplayName("InspirationalQuote Model")
    inner class InspirationalQuoteModel {

        @Test
        fun `isOlderThan24Hours should return true when older than 24 hours`() {
            // Given
            val oldQuote = createTestQuote(
                lastGeneratedAt = Instant.now().minusSeconds(25 * 60 * 60) // 25 hours ago
            )

            // Then
            oldQuote.isOlderThan24Hours() shouldBe true
        }

        @Test
        fun `isOlderThan24Hours should return false when newer than 24 hours`() {
            // Given
            val recentQuote = createTestQuote(
                lastGeneratedAt = Instant.now().minusSeconds(23 * 60 * 60) // 23 hours ago
            )

            // Then
            recentQuote.isOlderThan24Hours() shouldBe false
        }

        @Test
        fun `shouldRegenerate should return true when needsRegeneration is true`() {
            // Given
            val quote = createTestQuote(
                needsRegeneration = true,
                lastGeneratedAt = Instant.now() // Recent
            )

            // Then
            quote.shouldRegenerate() shouldBe true
        }

        @Test
        fun `shouldRegenerate should return true when older than 24 hours`() {
            // Given
            val quote = createTestQuote(
                needsRegeneration = false,
                lastGeneratedAt = Instant.now().minusSeconds(25 * 60 * 60)
            )

            // Then
            quote.shouldRegenerate() shouldBe true
        }

        @Test
        fun `shouldRegenerate should return false when recent and not flagged`() {
            // Given
            val quote = createTestQuote(
                needsRegeneration = false,
                lastGeneratedAt = Instant.now()
            )

            // Then
            quote.shouldRegenerate() shouldBe false
        }
    }
}
