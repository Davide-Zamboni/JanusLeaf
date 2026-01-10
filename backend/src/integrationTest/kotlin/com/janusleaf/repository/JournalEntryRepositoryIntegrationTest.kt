package com.janusleaf.repository

import com.janusleaf.model.JournalEntry
import com.janusleaf.model.User
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import org.springframework.data.domain.PageRequest
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDate
import java.util.*

/**
 * Integration tests for JournalEntryRepository using H2 in-memory database.
 */
@DataJpaTest
@ActiveProfiles("integrationTest")
@DisplayName("JournalEntryRepository Integration Tests")
class JournalEntryRepositoryIntegrationTest {

    @Autowired
    private lateinit var entityManager: TestEntityManager

    @Autowired
    private lateinit var journalEntryRepository: JournalEntryRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    private lateinit var testUser: User
    private lateinit var otherUser: User

    @BeforeEach
    fun setUp() {
        journalEntryRepository.deleteAll()
        userRepository.deleteAll()
        entityManager.flush()
        entityManager.clear()

        testUser = User(
            email = "test@example.com",
            username = "TestUser",
            passwordHash = "hashedPassword123"
        )
        entityManager.persistAndFlush(testUser)

        otherUser = User(
            email = "other@example.com",
            username = "OtherUser",
            passwordHash = "hashedPassword456"
        )
        entityManager.persistAndFlush(otherUser)

        entityManager.clear()
    }

    private fun createEntry(
        user: User = testUser,
        title: String = "Test Entry",
        body: String = "Test body",
        entryDate: LocalDate = LocalDate.now(),
        moodScore: Int? = null
    ): JournalEntry {
        val entry = JournalEntry(
            user = user,
            title = title,
            body = body,
            entryDate = entryDate,
            moodScore = moodScore
        )
        return entityManager.persistAndFlush(entry)
    }

    @Nested
    @DisplayName("findByUserId()")
    inner class FindByUserId {

        @Test
        fun `should return entries for user ordered by date descending`() {
            createEntry(title = "Jan 1", entryDate = LocalDate.of(2024, 1, 1))
            createEntry(title = "Jan 15", entryDate = LocalDate.of(2024, 1, 15))
            createEntry(title = "Jan 10", entryDate = LocalDate.of(2024, 1, 10))
            entityManager.clear()

            val page = journalEntryRepository.findByUserId(
                testUser.id,
                PageRequest.of(0, 10)
            )

            page.content shouldHaveSize 3
        }

        @Test
        fun `should not return entries from other users`() {
            createEntry(user = testUser, title = "User Entry")
            createEntry(user = otherUser, title = "Other User Entry")
            entityManager.clear()

            val page = journalEntryRepository.findByUserId(
                testUser.id,
                PageRequest.of(0, 10)
            )

            page.content shouldHaveSize 1
            page.content[0].title shouldBe "User Entry"
        }

        @Test
        fun `should return empty page when no entries exist`() {
            val page = journalEntryRepository.findByUserId(
                testUser.id,
                PageRequest.of(0, 10)
            )

            page.content shouldHaveSize 0
            page.totalElements shouldBe 0
        }

        @Test
        fun `should paginate correctly`() {
            repeat(5) { i ->
                createEntry(title = "Entry $i", entryDate = LocalDate.now().minusDays(i.toLong()))
            }
            entityManager.clear()

            val firstPage = journalEntryRepository.findByUserId(
                testUser.id,
                PageRequest.of(0, 2)
            )

            firstPage.content shouldHaveSize 2
            firstPage.totalElements shouldBe 5
            firstPage.totalPages shouldBe 3
            firstPage.hasNext().shouldBeTrue()

            val lastPage = journalEntryRepository.findByUserId(
                testUser.id,
                PageRequest.of(2, 2)
            )

            lastPage.content shouldHaveSize 1
            lastPage.hasNext().shouldBeFalse()
        }
    }

    @Nested
    @DisplayName("findByUserIdAndId()")
    inner class FindByUserIdAndId {

        @Test
        fun `should find entry when belongs to user`() {
            val entry = createEntry(title = "My Entry")
            entityManager.clear()

            val found = journalEntryRepository.findByUserIdAndId(testUser.id, entry.id)

            found.shouldNotBeNull()
            found.title shouldBe "My Entry"
        }

        @Test
        fun `should return null when entry belongs to different user`() {
            val entry = createEntry(user = otherUser, title = "Other's Entry")
            entityManager.clear()

            val found = journalEntryRepository.findByUserIdAndId(testUser.id, entry.id)

            found.shouldBeNull()
        }

        @Test
        fun `should return null when entry does not exist`() {
            val found = journalEntryRepository.findByUserIdAndId(testUser.id, UUID.randomUUID())

            found.shouldBeNull()
        }
    }

    @Nested
    @DisplayName("findByUserIdAndEntryDate()")
    inner class FindByUserIdAndEntryDate {

        @Test
        fun `should find entry by user and date`() {
            val specificDate = LocalDate.of(2024, 6, 15)
            createEntry(title = "June Entry", entryDate = specificDate)
            entityManager.clear()

            val found = journalEntryRepository.findByUserIdAndEntryDate(testUser.id, specificDate)

            found.shouldNotBeNull()
            found.title shouldBe "June Entry"
        }

        @Test
        fun `should return null when no entry for date`() {
            createEntry(entryDate = LocalDate.of(2024, 1, 1))
            entityManager.clear()

            val found = journalEntryRepository.findByUserIdAndEntryDate(
                testUser.id,
                LocalDate.of(2024, 12, 31)
            )

            found.shouldBeNull()
        }
    }

    @Nested
    @DisplayName("findByUserIdAndEntryDateBetweenOrderByEntryDateDescUpdatedAtDesc()")
    inner class FindByDateRange {

        @Test
        fun `should return entries within date range`() {
            createEntry(title = "Dec 2023", entryDate = LocalDate.of(2023, 12, 31))
            createEntry(title = "Jan 5", entryDate = LocalDate.of(2024, 1, 5))
            createEntry(title = "Jan 15", entryDate = LocalDate.of(2024, 1, 15))
            createEntry(title = "Jan 25", entryDate = LocalDate.of(2024, 1, 25))
            createEntry(title = "Feb 2024", entryDate = LocalDate.of(2024, 2, 1))
            entityManager.clear()

            val entries = journalEntryRepository.findByUserIdAndEntryDateBetweenOrderByEntryDateDescUpdatedAtDesc(
                testUser.id,
                LocalDate.of(2024, 1, 1),
                LocalDate.of(2024, 1, 31)
            )

            entries shouldHaveSize 3
            entries[0].title shouldBe "Jan 25"
            entries[1].title shouldBe "Jan 15"
            entries[2].title shouldBe "Jan 5"
        }

        @Test
        fun `should include entries on boundary dates`() {
            val startDate = LocalDate.of(2024, 1, 1)
            val endDate = LocalDate.of(2024, 1, 31)

            createEntry(title = "Start", entryDate = startDate)
            createEntry(title = "End", entryDate = endDate)
            entityManager.clear()

            val entries = journalEntryRepository.findByUserIdAndEntryDateBetweenOrderByEntryDateDescUpdatedAtDesc(
                testUser.id,
                startDate,
                endDate
            )

            entries shouldHaveSize 2
        }
    }

    @Nested
    @DisplayName("existsByUserIdAndId()")
    inner class ExistsByUserIdAndId {

        @Test
        fun `should return true when entry exists for user`() {
            val entry = createEntry()
            entityManager.clear()

            val exists = journalEntryRepository.existsByUserIdAndId(testUser.id, entry.id)

            exists.shouldBeTrue()
        }

        @Test
        fun `should return false when entry belongs to other user`() {
            val entry = createEntry(user = otherUser)
            entityManager.clear()

            val exists = journalEntryRepository.existsByUserIdAndId(testUser.id, entry.id)

            exists.shouldBeFalse()
        }

        @Test
        fun `should return false when entry does not exist`() {
            val exists = journalEntryRepository.existsByUserIdAndId(testUser.id, UUID.randomUUID())

            exists.shouldBeFalse()
        }
    }

    @Nested
    @DisplayName("deleteByUserIdAndId()")
    inner class DeleteByUserIdAndId {

        @Test
        fun `should delete entry when belongs to user`() {
            val entry = createEntry()
            val entryId = entry.id
            entityManager.clear()

            val deletedCount = journalEntryRepository.deleteByUserIdAndId(testUser.id, entryId)

            deletedCount shouldBe 1
            journalEntryRepository.existsById(entryId).shouldBeFalse()
        }

        @Test
        fun `should not delete entry belonging to other user`() {
            val entry = createEntry(user = otherUser)
            val entryId = entry.id
            entityManager.clear()

            val deletedCount = journalEntryRepository.deleteByUserIdAndId(testUser.id, entryId)

            deletedCount shouldBe 0
            journalEntryRepository.existsById(entryId).shouldBeTrue()
        }

        @Test
        fun `should return 0 when entry does not exist`() {
            val deletedCount = journalEntryRepository.deleteByUserIdAndId(testUser.id, UUID.randomUUID())

            deletedCount shouldBe 0
        }
    }

    @Nested
    @DisplayName("save()")
    inner class Save {

        @Test
        fun `should save new entry`() {
            val entry = JournalEntry(
                user = testUser,
                title = "New Entry",
                body = "Content",
                entryDate = LocalDate.now()
            )

            val saved = journalEntryRepository.save(entry)
            entityManager.flush()

            saved.id.shouldNotBeNull()
            saved.createdAt.shouldNotBeNull()
            saved.version shouldBe 0
        }

        @Test
        fun `should update existing entry`() {
            val entry = createEntry(body = "Original")
            entityManager.clear()

            val found = journalEntryRepository.findById(entry.id).orElseThrow()
            found.body = "Updated"
            journalEntryRepository.save(found)
            entityManager.flush()
            entityManager.clear()

            val reloaded = journalEntryRepository.findById(entry.id).orElseThrow()
            reloaded.body shouldBe "Updated"
        }

        @Test
        fun `should increment version on update`() {
            val entry = createEntry()
            entityManager.clear()
            entry.version shouldBe 0

            val found = journalEntryRepository.findById(entry.id).orElseThrow()
            found.body = "Modified"
            journalEntryRepository.save(found)
            entityManager.flush()
            entityManager.clear()

            val reloaded = journalEntryRepository.findById(entry.id).orElseThrow()
            reloaded.version shouldBe 1
        }
    }

    @Nested
    @DisplayName("Cascade Delete")
    inner class CascadeDelete {

        @Test
        fun `should delete entries when user is deleted`() {
            createEntry(user = testUser, title = "Entry 1")
            createEntry(user = testUser, title = "Entry 2")
            entityManager.clear()

            val initialCount = journalEntryRepository.count()
            initialCount shouldBe 2

            userRepository.deleteById(testUser.id)
            entityManager.flush()
            entityManager.clear()

            journalEntryRepository.count() shouldBe 0
        }
    }
}
