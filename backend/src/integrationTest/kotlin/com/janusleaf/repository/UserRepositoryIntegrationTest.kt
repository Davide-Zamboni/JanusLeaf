package com.janusleaf.repository

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
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import org.springframework.test.context.ActiveProfiles
import java.util.*

/**
 * Integration tests for UserRepository using H2 in-memory database.
 */
@DataJpaTest
@ActiveProfiles("integrationTest")
@DisplayName("UserRepository Integration Tests")
class UserRepositoryIntegrationTest {

    @Autowired
    private lateinit var entityManager: TestEntityManager

    @Autowired
    private lateinit var userRepository: UserRepository

    private lateinit var testUser: User

    @BeforeEach
    fun setUp() {
        userRepository.deleteAll()
        entityManager.flush()
        entityManager.clear()
        
        testUser = User(
            email = "test@example.com",
            username = "TestUser",
            passwordHash = "hashedPassword123"
        )
        entityManager.persistAndFlush(testUser)
        entityManager.clear()
    }

    @Nested
    @DisplayName("findByEmail()")
    inner class FindByEmail {

        @Test
        fun `should find user by email when exists`() {
            val foundUser = userRepository.findByEmail("test@example.com")

            foundUser.shouldNotBeNull()
            foundUser.id shouldBe testUser.id
            foundUser.email shouldBe testUser.email
            foundUser.username shouldBe testUser.username
        }

        @Test
        fun `should return null when email not found`() {
            val foundUser = userRepository.findByEmail("nonexistent@example.com")
            foundUser.shouldBeNull()
        }
    }

    @Nested
    @DisplayName("existsByEmail()")
    inner class ExistsByEmail {

        @Test
        fun `should return true when email exists`() {
            val exists = userRepository.existsByEmail("test@example.com")
            exists.shouldBeTrue()
        }

        @Test
        fun `should return false when email does not exist`() {
            val exists = userRepository.existsByEmail("nonexistent@example.com")
            exists.shouldBeFalse()
        }
    }

    @Nested
    @DisplayName("save()")
    inner class Save {

        @Test
        fun `should save new user`() {
            val newUser = User(
                email = "newuser@example.com",
                username = "NewUser",
                passwordHash = "hash123"
            )

            val savedUser = userRepository.save(newUser)
            entityManager.flush()

            savedUser.id.shouldNotBeNull()
            savedUser.email shouldBe "newuser@example.com"
            savedUser.createdAt.shouldNotBeNull()
        }

        @Test
        fun `should update existing user`() {
            val user = userRepository.findById(testUser.id).orElseThrow()
            user.username = "UpdatedUsername"

            userRepository.save(user)
            entityManager.flush()
            entityManager.clear()

            val foundUser = userRepository.findById(testUser.id).orElseThrow()
            foundUser.username shouldBe "UpdatedUsername"
        }
    }

    @Nested
    @DisplayName("findById()")
    inner class FindById {

        @Test
        fun `should find user by id when exists`() {
            val foundUser = userRepository.findById(testUser.id)

            foundUser.isPresent.shouldBeTrue()
            foundUser.get().email shouldBe testUser.email
        }

        @Test
        fun `should return empty when id not found`() {
            val foundUser = userRepository.findById(UUID.randomUUID())
            foundUser.isEmpty.shouldBeTrue()
        }
    }

    @Nested
    @DisplayName("delete()")
    inner class Delete {

        @Test
        fun `should delete user`() {
            val userId = testUser.id
            userRepository.existsById(userId).shouldBeTrue()

            userRepository.deleteById(userId)
            entityManager.flush()
            entityManager.clear()

            userRepository.existsById(userId).shouldBeFalse()
        }
    }
}
