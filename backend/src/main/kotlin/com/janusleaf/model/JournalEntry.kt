package com.janusleaf.model

import jakarta.persistence.*
import org.hibernate.annotations.OnDelete
import org.hibernate.annotations.OnDeleteAction
import java.time.Instant
import java.time.LocalDate
import java.util.*

@Entity
@Table(name = "journal_entries")
class JournalEntry(
    @Id
    val id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    val user: User,

    @Column(nullable = false)
    var title: String,

    @Column(columnDefinition = "TEXT")
    var body: String = "",

    @Column(name = "mood_score")
    var moodScore: Int? = null,

    @Column(name = "entry_date", nullable = false)
    val entryDate: LocalDate = LocalDate.now(),

    @Version
    @Column(name = "version", nullable = false)
    var version: Long = 0,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()
) {
    @PreUpdate
    fun onUpdate() {
        updatedAt = Instant.now()
    }

    companion object {
        fun defaultTitle(): String = LocalDate.now().toString()
    }
}
