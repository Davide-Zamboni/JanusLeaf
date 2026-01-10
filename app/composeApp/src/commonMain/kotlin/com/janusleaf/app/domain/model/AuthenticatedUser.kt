package com.janusleaf.app.domain.model

/**
 * Domain model representing a successfully authenticated user with tokens.
 */
data class AuthenticatedUser(
    val user: User,
    val tokens: AuthTokens
)
