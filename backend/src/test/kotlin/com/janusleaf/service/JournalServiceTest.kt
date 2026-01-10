package com.janusleaf.service

import com.janusleaf.dto.*
import com.janusleaf.exception.NoteNotFoundException
import com.janusleaf.model.JournalEntry
import com.janusleaf.model.User
import com.janusleaf.repository.JournalEntryRepository
import com.janusleaf.repository.UserRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.mockk.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.orm.ObjectOptimisticLockingFailureException
import java.time.Instant
import java.time.LocalDate
import java.util.*

@DisplayName("JournalService")
class JournalServiceTest {

    private lateinit var journalEntryRepository: JournalEntryRepository
    private lateinit var userRepository: UserRepository
    private lateinit var moodAnalysisService: MoodAnalysisService
    private lateinit var journalService: JournalService

    private val testUserId = UUID.randomUUID()
    private val testUser = User(
        id = testUserId,
        email = "test@example.com",
        username = "TestUser",
        passwordHash = "hashedPassword123",
        createdAt = Instant.now(),
        updatedAt = Instant.now()
    )

    private fun createTestEntry(
        id: UUID = UUID.randomUUID(),
        title: String = "Test Entry",
        body: String = "Test body content",
        moodScore: Int? = null,
        entryDate: LocalDate = LocalDate.now(),
        version: Long = 0
    ): JournalEntry {
        return JournalEntry(
            id = id,
            user = testUser,
            title = title,
            body = body,
            moodScore = moodScore,
            entryDate = entryDate,
            version = version
        )
    }

    @BeforeEach
    fun setUp() {
        journalEntryRepository = mockk()
        userRepository = mockk()
        moodAnalysisService = mockk(relaxed = true) // Relaxed mock - we don't care about mood analysis in most tests
        journalService = JournalService(journalEntryRepository, userRepository, moodAnalysisService)
    }

    @Nested
    @DisplayName("createEntry()")
    inner class CreateEntry {

        @Test
        fun `should create entry with provided title`() {
            // Given
            val request = CreateJournalEntryRequest(
                title = "My Day",
                body = "Today was great!",
                entryDate = LocalDate.of(2024, 1, 15)
            )

            every { userRepository.getReferenceById(testUserId) } returns testUser
            every { journalEntryRepository.save(any()) } answers { firstArg() }

            // When
            val response = journalService.createEntry(testUserId, request)

            // Then
            response.title shouldBe "My Day"
            response.body shouldBe "Today was great!"
            response.entryDate shouldBe LocalDate.of(2024, 1, 15)
            response.moodScore shouldBe null

            verify { journalEntryRepository.save(any()) }
        }

        @Test
        fun `should use today's date as title when not provided`() {
            // Given
            val today = LocalDate.now()
            val request = CreateJournalEntryRequest(
                title = null,
                body = "No title entry"
            )

            every { userRepository.getReferenceById(testUserId) } returns testUser
            every { journalEntryRepository.save(any()) } answers { firstArg() }

            // When
            val response = journalService.createEntry(testUserId, request)

            // Then
            response.title shouldBe today.toString()
            response.entryDate shouldBe today
        }

        @Test
        fun `should use today's date as title when title is blank`() {
            // Given
            val today = LocalDate.now()
            val request = CreateJournalEntryRequest(
                title = "   ",
                body = "Entry with blank title"
            )

            every { userRepository.getReferenceById(testUserId) } returns testUser
            every { journalEntryRepository.save(any()) } answers { firstArg() }

            // When
            val response = journalService.createEntry(testUserId, request)

            // Then
            response.title shouldBe today.toString()
        }

        @Test
        fun `should create entry with empty body when not provided`() {
            // Given
            val request = CreateJournalEntryRequest()

            every { userRepository.getReferenceById(testUserId) } returns testUser
            every { journalEntryRepository.save(any()) } answers { firstArg() }

            // When
            val response = journalService.createEntry(testUserId, request)

            // Then
            response.body shouldBe ""
        }

        @Test
        fun `should trim title and body`() {
            // Given
            val request = CreateJournalEntryRequest(
                title = "  My Title  ",
                body = "  My content  "
            )

            every { userRepository.getReferenceById(testUserId) } returns testUser
            every { journalEntryRepository.save(any()) } answers { firstArg() }

            // When
            val response = journalService.createEntry(testUserId, request)

            // Then
            response.title shouldBe "My Title"
            response.body shouldBe "My content"
        }
    }

    @Nested
    @DisplayName("getEntry()")
    inner class GetEntry {

        @Test
        fun `should return entry when found and belongs to user`() {
            // Given
            val entryId = UUID.randomUUID()
            val entry = createTestEntry(id = entryId, title = "Found Entry")

            every { journalEntryRepository.findByUserIdAndId(testUserId, entryId) } returns entry

            // When
            val response = journalService.getEntry(testUserId, entryId)

            // Then
            response.id shouldBe entryId
            response.title shouldBe "Found Entry"
        }

        @Test
        fun `should throw NoteNotFoundException when entry not found`() {
            // Given
            val entryId = UUID.randomUUID()
            every { journalEntryRepository.findByUserIdAndId(testUserId, entryId) } returns null

            // When/Then
            val exception = shouldThrow<NoteNotFoundException> {
                journalService.getEntry(testUserId, entryId)
            }
            
            exception.message shouldBe "Journal entry not found"
        }

        @Test
        fun `should not return entry belonging to another user`() {
            // Given
            val entryId = UUID.randomUUID()
            val otherUserId = UUID.randomUUID()
            
            every { journalEntryRepository.findByUserIdAndId(otherUserId, entryId) } returns null

            // When/Then
            shouldThrow<NoteNotFoundException> {
                journalService.getEntry(otherUserId, entryId)
            }
        }
    }

    @Nested
    @DisplayName("getEntries()")
    inner class GetEntries {

        @Test
        fun `should return paginated entries`() {
            // Given
            val entries = listOf(
                createTestEntry(title = "Entry 1"),
                createTestEntry(title = "Entry 2")
            )
            val page = PageImpl(entries, PageRequest.of(0, 10), 2)

            every { journalEntryRepository.findByUserId(testUserId, any()) } returns page

            // When
            val response = journalService.getEntries(testUserId, 0, 10)

            // Then
            response.entries.size shouldBe 2
            response.page shouldBe 0
            response.size shouldBe 10
            response.totalElements shouldBe 2
            response.totalPages shouldBe 1
            response.hasNext shouldBe false
            response.hasPrevious shouldBe false
        }

        @Test
        fun `should truncate body preview for long entries`() {
            // Given
            val longBody = "A".repeat(200)
            val entry = createTestEntry(body = longBody)
            val page = PageImpl(listOf(entry), PageRequest.of(0, 10), 1)

            every { journalEntryRepository.findByUserId(testUserId, any()) } returns page

            // When
            val response = journalService.getEntries(testUserId, 0, 10)

            // Then
            response.entries[0].bodyPreview.length shouldBe 153 // 150 + "..."
            response.entries[0].bodyPreview shouldContain "..."
        }
    }

    @Nested
    @DisplayName("getEntriesByDateRange()")
    inner class GetEntriesByDateRange {

        @Test
        fun `should return entries within date range`() {
            // Given
            val startDate = LocalDate.of(2024, 1, 1)
            val endDate = LocalDate.of(2024, 1, 31)
            val entries = listOf(
                createTestEntry(entryDate = LocalDate.of(2024, 1, 10)),
                createTestEntry(entryDate = LocalDate.of(2024, 1, 20))
            )

            every { 
                journalEntryRepository.findByUserIdAndEntryDateBetweenOrderByEntryDateDescUpdatedAtDesc(
                    testUserId, startDate, endDate
                ) 
            } returns entries

            // When
            val response = journalService.getEntriesByDateRange(testUserId, startDate, endDate)

            // Then
            response.size shouldBe 2
        }
    }

    @Nested
    @DisplayName("updateBody()")
    inner class UpdateBody {

        @Test
        fun `should update body successfully`() {
            // Given
            val entryId = UUID.randomUUID()
            val entry = createTestEntry(id = entryId, body = "Old content", version = 1)
            val request = UpdateJournalBodyRequest(body = "New content")

            every { journalEntryRepository.findByUserIdAndId(testUserId, entryId) } returns entry
            every { journalEntryRepository.saveAndFlush(any()) } answers { firstArg() }
            every { journalEntryRepository.resetMoodScore(entryId) } returns 1

            // When
            val response = journalService.updateBody(testUserId, entryId, request)

            // Then
            response.body shouldBe "New content"
            verify { journalEntryRepository.saveAndFlush(match { it.body == "New content" }) }
        }

        @Test
        fun `should succeed when expected version matches`() {
            // Given
            val entryId = UUID.randomUUID()
            val entry = createTestEntry(id = entryId, version = 5)
            val request = UpdateJournalBodyRequest(body = "Updated", expectedVersion = 5)

            every { journalEntryRepository.findByUserIdAndId(testUserId, entryId) } returns entry
            every { journalEntryRepository.saveAndFlush(any()) } answers { firstArg() }
            every { journalEntryRepository.resetMoodScore(entryId) } returns 1

            // When
            val response = journalService.updateBody(testUserId, entryId, request)

            // Then
            response.body shouldBe "Updated"
        }

        @Test
        fun `should throw when expected version does not match`() {
            // Given
            val entryId = UUID.randomUUID()
            val entry = createTestEntry(id = entryId, version = 5)
            val request = UpdateJournalBodyRequest(body = "Updated", expectedVersion = 3)

            every { journalEntryRepository.findByUserIdAndId(testUserId, entryId) } returns entry

            // When/Then
            val exception = shouldThrow<ObjectOptimisticLockingFailureException> {
                journalService.updateBody(testUserId, entryId, request)
            }
            
            exception.message shouldContain "Expected version: 3, current version: 5"
            verify(exactly = 0) { journalEntryRepository.save(any()) }
        }

        @Test
        fun `should throw NoteNotFoundException when entry not found`() {
            // Given
            val entryId = UUID.randomUUID()
            val request = UpdateJournalBodyRequest(body = "Content")

            every { journalEntryRepository.findByUserIdAndId(testUserId, entryId) } returns null

            // When/Then
            shouldThrow<NoteNotFoundException> {
                journalService.updateBody(testUserId, entryId, request)
            }
        }
    }

    @Nested
    @DisplayName("updateMetadata()")
    inner class UpdateMetadata {

        @Test
        fun `should update title successfully`() {
            // Given
            val entryId = UUID.randomUUID()
            val entry = createTestEntry(id = entryId, title = "Old Title")
            val request = UpdateJournalMetadataRequest(title = "New Title")

            every { journalEntryRepository.findByUserIdAndId(testUserId, entryId) } returns entry
            every { journalEntryRepository.save(any()) } answers { firstArg() }

            // When
            val response = journalService.updateMetadata(testUserId, entryId, request)

            // Then
            response.title shouldBe "New Title"
        }

        @Test
        fun `should not change fields when not provided`() {
            // Given
            val entryId = UUID.randomUUID()
            val entry = createTestEntry(id = entryId, title = "Original", moodScore = 5)
            val request = UpdateJournalMetadataRequest()

            every { journalEntryRepository.findByUserIdAndId(testUserId, entryId) } returns entry
            every { journalEntryRepository.save(any()) } answers { firstArg() }

            // When
            val response = journalService.updateMetadata(testUserId, entryId, request)

            // Then
            response.title shouldBe "Original"
            response.moodScore shouldBe 5
        }

        @Test
        fun `should trim title`() {
            // Given
            val entryId = UUID.randomUUID()
            val entry = createTestEntry(id = entryId)
            val request = UpdateJournalMetadataRequest(title = "  Trimmed Title  ")

            every { journalEntryRepository.findByUserIdAndId(testUserId, entryId) } returns entry
            every { journalEntryRepository.save(any()) } answers { firstArg() }

            // When
            val response = journalService.updateMetadata(testUserId, entryId, request)

            // Then
            response.title shouldBe "Trimmed Title"
        }
    }

    @Nested
    @DisplayName("deleteEntry()")
    inner class DeleteEntry {

        @Test
        fun `should delete entry successfully`() {
            // Given
            val entryId = UUID.randomUUID()

            every { journalEntryRepository.existsByUserIdAndId(testUserId, entryId) } returns true
            every { journalEntryRepository.deleteByUserIdAndId(testUserId, entryId) } returns 1

            // When
            journalService.deleteEntry(testUserId, entryId)

            // Then
            verify { journalEntryRepository.deleteByUserIdAndId(testUserId, entryId) }
        }

        @Test
        fun `should throw NoteNotFoundException when entry not found`() {
            // Given
            val entryId = UUID.randomUUID()

            every { journalEntryRepository.existsByUserIdAndId(testUserId, entryId) } returns false

            // When/Then
            shouldThrow<NoteNotFoundException> {
                journalService.deleteEntry(testUserId, entryId)
            }

            verify(exactly = 0) { journalEntryRepository.deleteByUserIdAndId(any(), any()) }
        }
    }
}
