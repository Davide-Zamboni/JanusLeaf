package com.janusleaf.model

import jakarta.persistence.*
import org.hibernate.annotations.OnDelete
import org.hibernate.annotations.OnDeleteAction
import java.time.Instant
import java.util.*

/**
 * Converter to store String array as comma-separated string.
 * Works with both PostgreSQL and H2 databases.
 */
@Converter
class StringArrayConverter : AttributeConverter<Array<String>, String> {
    override fun convertToDatabaseColumn(attribute: Array<String>?): String {
        return attribute?.joinToString(",") ?: ""
    }

    override fun convertToEntityAttribute(dbData: String?): Array<String> {
        return if (dbData.isNullOrBlank()) {
            arrayOf()
        } else {
            dbData.split(",").toTypedArray()
        }
    }
}

/**
 * Stores an AI-generated inspirational quote for a user.
 * 
 * Each user has at most one quote (newest overwrites previous).
 * Quotes are generated based on the user's last 20 journal entries.
 * 
 * Regeneration triggers:
 * - When user creates a new journal entry (needsRegeneration = true)
 * - Once per day (checked via lastGeneratedAt)
 * - When user has no quote yet
 */
@Entity
@Table(name = "inspirational_quotes")
class InspirationalQuote(
    @Id
    val id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    @OnDelete(action = OnDeleteAction.CASCADE)
    val user: User,

    @Column(nullable = false, columnDefinition = "TEXT")
    var quote: String,

    /**
     * Array of 4 tags extracted from journal themes.
     * Stored as comma-separated string for database compatibility.
     */
    @Column(nullable = false)
    @Convert(converter = StringArrayConverter::class)
    var tags: Array<String> = arrayOf(),

    /**
     * Flag indicating this quote needs regeneration.
     * Set to true when user creates a new journal entry.
     */
    @Column(name = "needs_regeneration", nullable = false)
    var needsRegeneration: Boolean = false,

    /**
     * When this quote was last generated.
     * Used for daily regeneration check.
     */
    @Column(name = "last_generated_at", nullable = false)
    var lastGeneratedAt: Instant = Instant.now(),

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()
) {
    @PreUpdate
    fun onUpdate() {
        updatedAt = Instant.now()
    }

    /**
     * Check if this quote should be regenerated based on age (24 hours).
     */
    fun isOlderThan24Hours(): Boolean {
        return Instant.now().isAfter(lastGeneratedAt.plusSeconds(24 * 60 * 60))
    }

    /**
     * Check if regeneration is needed.
     */
    fun shouldRegenerate(): Boolean {
        return needsRegeneration || isOlderThan24Hours()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is InspirationalQuote) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}
