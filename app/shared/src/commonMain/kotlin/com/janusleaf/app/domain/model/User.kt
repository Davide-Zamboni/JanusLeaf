package com.janusleaf.app.domain.model

import kotlinx.datetime.Instant

/**
 * Domain model representing a user in the JanusLeaf app.
 */
data class User(
    val id: String,
    val email: String,
    val username: String,
    val createdAt: Instant,
    val updatedAt: Instant
)
