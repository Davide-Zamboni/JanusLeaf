package com.janusleaf.repository

import com.janusleaf.model.InspirationalQuote
import com.janusleaf.model.JournalEntry
import com.janusleaf.model.User
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldContain
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
import org.springframework.test.context.ActiveProfiles
import java.time.Instant
import java.time.LocalDate
import java.util.*

/**
 * Integration tests for InspirationalQuoteRepository using H2 in-memory database.
 */
@DataJpaTest
@ActiveProfiles("integrationTest")
@DisplayName("InspirationalQuoteRepository Integration Tests")
class InspirationalQuoteRepositoryIntegrationTest {

    @Autowired
    private lateinit var entityManager: TestEntityManager

    @Autowired
    private lateinit var inspirationalQuoteRepository: InspirationalQuoteRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var journalEntryRepository: JournalEntryRepository

    private lateinit var testUser: User
    private lateinit var otherUser: User

    @BeforeEach
    fun setUp() {
        inspirationalQuoteRepository.deleteAll()
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

    private fun createQuote(
        user: User = testUser,
        quote: String = "Test quote",
        tags: Array<String> = arrayOf("tag1", "tag2", "tag3", "tag4"),
        needsRegeneration: Boolean = false,
        lastGeneratedAt: Instant = Instant.now()
    ): InspirationalQuote {
        val inspirationalQuote = InspirationalQuote(
            user = user,
            quote = quote,
            tags = tags,
            needsRegeneration = needsRegeneration,
            lastGeneratedAt = lastGeneratedAt
        )
        return entityManager.persistAndFlush(inspirationalQuote)
    }

    private fun createJournalEntry(
        user: User = testUser,
        title: String = "Test Entry",
        body: String = "Test body"
    ): JournalEntry {
        val entry = JournalEntry(
            user = user,
            title = title,
            body = body,
            entryDate = LocalDate.now()
        )
        return entityManager.persistAndFlush(entry)
    }

    @Nested
    @DisplayName("findByUserId()")
    inner class FindByUserId {

        @Test
        fun `should find quote when exists for user`() {
            createQuote(quote = "My inspirational quote")
            entityManager.clear()

            val found = inspirationalQuoteRepository.findByUserId(testUser.id)

            found.shouldNotBeNull()
            found.quote shouldBe "My inspirational quote"
        }

        @Test
        fun `should return null when no quote exists for user`() {
            entityManager.clear()

            val found = inspirationalQuoteRepository.findByUserId(testUser.id)

            found.shouldBeNull()
        }

        @Test
        fun `should not return quote from other user`() {
            createQuote(user = otherUser, quote = "Other's quote")
            entityManager.clear()

            val found = inspirationalQuoteRepository.findByUserId(testUser.id)

            found.shouldBeNull()
        }
    }

    @Nested
    @DisplayName("existsByUserId()")
    inner class ExistsByUserId {

        @Test
        fun `should return true when quote exists for user`() {
            createQuote()
            entityManager.clear()

            val exists = inspirationalQuoteRepository.existsByUserId(testUser.id)

            exists.shouldBeTrue()
        }

        @Test
        fun `should return false when no quote exists for user`() {
            val exists = inspirationalQuoteRepository.existsByUserId(testUser.id)

            exists.shouldBeFalse()
        }

        @Test
        fun `should return false when quote exists only for other user`() {
            createQuote(user = otherUser)
            entityManager.clear()

            val exists = inspirationalQuoteRepository.existsByUserId(testUser.id)

            exists.shouldBeFalse()
        }
    }

    @Nested
    @DisplayName("findQuotesNeedingRegeneration()")
    inner class FindQuotesNeedingRegeneration {

        @Test
        fun `should find quotes with needsRegeneration flag set`() {
            createQuote(user = testUser, needsRegeneration = true, lastGeneratedAt = Instant.now())
            entityManager.clear()

            val cutoff = Instant.now().minusSeconds(24 * 60 * 60)
            val quotes = inspirationalQuoteRepository.findQuotesNeedingRegeneration(cutoff)

            quotes shouldHaveSize 1
            quotes[0].needsRegeneration shouldBe true
        }

        @Test
        fun `should find quotes older than cutoff time`() {
            val oldTime = Instant.now().minusSeconds(48 * 60 * 60) // 48 hours ago
            createQuote(user = testUser, needsRegeneration = false, lastGeneratedAt = oldTime)
            entityManager.clear()

            val cutoff = Instant.now().minusSeconds(24 * 60 * 60) // 24 hours ago
            val quotes = inspirationalQuoteRepository.findQuotesNeedingRegeneration(cutoff)

            quotes shouldHaveSize 1
        }

        @Test
        fun `should not find recent quotes without regeneration flag`() {
            createQuote(user = testUser, needsRegeneration = false, lastGeneratedAt = Instant.now())
            entityManager.clear()

            val cutoff = Instant.now().minusSeconds(24 * 60 * 60)
            val quotes = inspirationalQuoteRepository.findQuotesNeedingRegeneration(cutoff)

            quotes shouldHaveSize 0
        }

        @Test
        fun `should order by lastGeneratedAt ascending`() {
            val older = Instant.now().minusSeconds(100000)
            val newer = Instant.now().minusSeconds(50000)
            
            createQuote(user = testUser, needsRegeneration = true, lastGeneratedAt = newer)
            createQuote(user = otherUser, needsRegeneration = true, lastGeneratedAt = older)
            entityManager.clear()

            val cutoff = Instant.now().minusSeconds(24 * 60 * 60)
            val quotes = inspirationalQuoteRepository.findQuotesNeedingRegeneration(cutoff)

            quotes shouldHaveSize 2
            quotes[0].user.id shouldBe otherUser.id // Older first
            quotes[1].user.id shouldBe testUser.id
        }
    }

    @Nested
    @DisplayName("markForRegeneration()")
    inner class MarkForRegeneration {

        @Test
        fun `should mark quote for regeneration`() {
            val quote = createQuote(needsRegeneration = false)
            entityManager.clear()
            quote.needsRegeneration.shouldBeFalse()

            val updated = inspirationalQuoteRepository.markForRegeneration(testUser.id)
            entityManager.flush()
            entityManager.clear()

            updated shouldBe 1
            val reloaded = inspirationalQuoteRepository.findByUserId(testUser.id)
            reloaded.shouldNotBeNull()
            reloaded.needsRegeneration.shouldBeTrue()
        }

        @Test
        fun `should return 0 when no quote exists for user`() {
            val updated = inspirationalQuoteRepository.markForRegeneration(testUser.id)

            updated shouldBe 0
        }

        @Test
        fun `should not affect other user's quote`() {
            createQuote(user = testUser, needsRegeneration = false)
            createQuote(user = otherUser, needsRegeneration = false)
            entityManager.clear()

            inspirationalQuoteRepository.markForRegeneration(testUser.id)
            entityManager.flush()
            entityManager.clear()

            val otherQuote = inspirationalQuoteRepository.findByUserId(otherUser.id)
            otherQuote.shouldNotBeNull()
            otherQuote.needsRegeneration.shouldBeFalse()
        }
    }

    @Nested
    @DisplayName("findUserIdsWithoutQuotes()")
    inner class FindUserIdsWithoutQuotes {

        @Test
        fun `should find users with journal entries but no quotes`() {
            createJournalEntry(user = testUser)
            entityManager.clear()

            val userIds = inspirationalQuoteRepository.findUserIdsWithoutQuotes()

            userIds shouldHaveSize 1
            userIds shouldContain testUser.id
        }

        @Test
        fun `should not include users who already have quotes`() {
            createJournalEntry(user = testUser)
            createQuote(user = testUser)
            entityManager.clear()

            val userIds = inspirationalQuoteRepository.findUserIdsWithoutQuotes()

            userIds shouldHaveSize 0
        }

        @Test
        fun `should not include users without journal entries`() {
            // No journal entries created
            entityManager.clear()

            val userIds = inspirationalQuoteRepository.findUserIdsWithoutQuotes()

            userIds shouldHaveSize 0
        }

        @Test
        fun `should find multiple users without quotes`() {
            createJournalEntry(user = testUser)
            createJournalEntry(user = otherUser)
            entityManager.clear()

            val userIds = inspirationalQuoteRepository.findUserIdsWithoutQuotes()

            userIds shouldHaveSize 2
            userIds shouldContain testUser.id
            userIds shouldContain otherUser.id
        }
    }

    @Nested
    @DisplayName("deleteByUserId()")
    inner class DeleteByUserId {

        @Test
        fun `should delete quote for user`() {
            createQuote(user = testUser)
            entityManager.clear()

            val deleted = inspirationalQuoteRepository.deleteByUserId(testUser.id)
            entityManager.flush()

            deleted shouldBe 1
            inspirationalQuoteRepository.existsByUserId(testUser.id).shouldBeFalse()
        }

        @Test
        fun `should return 0 when no quote exists`() {
            val deleted = inspirationalQuoteRepository.deleteByUserId(testUser.id)

            deleted shouldBe 0
        }

        @Test
        fun `should not delete other user's quote`() {
            createQuote(user = otherUser)
            entityManager.clear()

            val deleted = inspirationalQuoteRepository.deleteByUserId(testUser.id)
            entityManager.flush()

            deleted shouldBe 0
            inspirationalQuoteRepository.existsByUserId(otherUser.id).shouldBeTrue()
        }
    }

    @Nested
    @DisplayName("save()")
    inner class Save {

        @Test
        fun `should save new quote`() {
            val quote = InspirationalQuote(
                user = testUser,
                quote = "New inspirational quote",
                tags = arrayOf("hope", "courage", "growth", "peace"),
                needsRegeneration = false,
                lastGeneratedAt = Instant.now()
            )

            val saved = inspirationalQuoteRepository.save(quote)
            entityManager.flush()

            saved.id.shouldNotBeNull()
            saved.createdAt.shouldNotBeNull()
        }

        @Test
        fun `should update existing quote`() {
            val quote = createQuote(quote = "Original quote")
            entityManager.clear()

            val found = inspirationalQuoteRepository.findById(quote.id).orElseThrow()
            found.quote = "Updated quote"
            found.tags = arrayOf("new", "tags", "here", "now")
            inspirationalQuoteRepository.save(found)
            entityManager.flush()
            entityManager.clear()

            val reloaded = inspirationalQuoteRepository.findById(quote.id).orElseThrow()
            reloaded.quote shouldBe "Updated quote"
            reloaded.tags shouldBe arrayOf("new", "tags", "here", "now")
        }
    }

    @Nested
    @DisplayName("Cascade Delete")
    inner class CascadeDelete {

        @Test
        fun `should delete quote when user is deleted`() {
            createQuote(user = testUser)
            entityManager.clear()

            inspirationalQuoteRepository.existsByUserId(testUser.id).shouldBeTrue()

            userRepository.deleteById(testUser.id)
            entityManager.flush()
            entityManager.clear()

            inspirationalQuoteRepository.count() shouldBe 0
        }
    }
}
