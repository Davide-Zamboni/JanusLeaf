package com.janusleaf.repository

import com.janusleaf.config.TestContainersConfig
import com.janusleaf.model.User
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.testcontainers.junit.jupiter.Testcontainers
import java.time.Instant
import java.util.*

@DataJpaTest
@Testcontainers
@Import(TestContainersConfig::class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@DisplayName("UserRepository")
class UserRepositoryTest {

    @Autowired
    private lateinit var entityManager: TestEntityManager

    @Autowired
    private lateinit var userRepository: UserRepository

    private lateinit var testUser: User

    @BeforeEach
    fun setUp() {
        // Clear existing data
        userRepository.deleteAll()
        
        // Create and persist a test user
        testUser = User(
            email = "test@example.com",
            username = "TestUser",
            passwordHash = "hashedPassword123"
        )
        entityManager.persistAndFlush(testUser)
    }

    @Nested
    @DisplayName("findByEmail()")
    inner class FindByEmail {

        @Test
        fun `should find user by email when exists`() {
            // When
            val foundUser = userRepository.findByEmail("test@example.com")

            // Then
            foundUser.shouldNotBeNull()
            foundUser.id shouldBe testUser.id
            foundUser.email shouldBe testUser.email
            foundUser.username shouldBe testUser.username
        }

        @Test
        fun `should return null when email not found`() {
            // When
            val foundUser = userRepository.findByEmail("nonexistent@example.com")

            // Then
            foundUser.shouldBeNull()
        }

        @Test
        fun `should be case sensitive for email lookup`() {
            // When
            val foundUser = userRepository.findByEmail("TEST@EXAMPLE.COM")

            // Then
            foundUser.shouldBeNull() // Because we stored lowercase
        }
    }

    @Nested
    @DisplayName("existsByEmail()")
    inner class ExistsByEmail {

        @Test
        fun `should return true when email exists`() {
            // When
            val exists = userRepository.existsByEmail("test@example.com")

            // Then
            exists.shouldBeTrue()
        }

        @Test
        fun `should return false when email does not exist`() {
            // When
            val exists = userRepository.existsByEmail("nonexistent@example.com")

            // Then
            exists.shouldBeFalse()
        }
    }

    @Nested
    @DisplayName("save()")
    inner class Save {

        @Test
        fun `should save new user`() {
            // Given
            val newUser = User(
                email = "newuser@example.com",
                username = "NewUser",
                passwordHash = "hash123"
            )

            // When
            val savedUser = userRepository.save(newUser)
            entityManager.flush()

            // Then
            savedUser.id.shouldNotBeNull()
            savedUser.email shouldBe "newuser@example.com"
            savedUser.createdAt.shouldNotBeNull()
        }

        @Test
        fun `should update existing user`() {
            // Given
            testUser.username = "UpdatedUsername"

            // When
            val updatedUser = userRepository.save(testUser)
            entityManager.flush()
            entityManager.clear()

            // Then
            val foundUser = userRepository.findById(testUser.id).orElseThrow()
            foundUser.username shouldBe "UpdatedUsername"
        }
    }

    @Nested
    @DisplayName("findById()")
    inner class FindById {

        @Test
        fun `should find user by id when exists`() {
            // When
            val foundUser = userRepository.findById(testUser.id)

            // Then
            foundUser.isPresent.shouldBeTrue()
            foundUser.get().email shouldBe testUser.email
        }

        @Test
        fun `should return empty when id not found`() {
            // When
            val foundUser = userRepository.findById(UUID.randomUUID())

            // Then
            foundUser.isEmpty.shouldBeTrue()
        }
    }

    @Nested
    @DisplayName("delete()")
    inner class Delete {

        @Test
        fun `should delete user`() {
            // Given
            val userId = testUser.id
            userRepository.existsById(userId).shouldBeTrue()

            // When
            userRepository.delete(testUser)
            entityManager.flush()

            // Then
            userRepository.existsById(userId).shouldBeFalse()
        }
    }

    @Nested
    @DisplayName("Entity Constraints")
    inner class EntityConstraints {

        @Test
        fun `should enforce unique email constraint`() {
            // Given
            val duplicateUser = User(
                email = "test@example.com", // Same email as testUser
                username = "DuplicateUser",
                passwordHash = "hash123"
            )

            // When/Then
            try {
                userRepository.saveAndFlush(duplicateUser)
                throw AssertionError("Should have thrown an exception for duplicate email")
            } catch (e: Exception) {
                // Expected - unique constraint violation
            }
        }
    }

    @Nested
    @DisplayName("Timestamps")
    inner class Timestamps {

        @Test
        fun `should set createdAt on new user`() {
            // Given
            val newUser = User(
                email = "timestamps@example.com",
                username = "TimestampUser",
                passwordHash = "hash123"
            )

            // When
            val savedUser = userRepository.saveAndFlush(newUser)

            // Then
            savedUser.createdAt.shouldNotBeNull()
            savedUser.updatedAt.shouldNotBeNull()
        }
    }
}
