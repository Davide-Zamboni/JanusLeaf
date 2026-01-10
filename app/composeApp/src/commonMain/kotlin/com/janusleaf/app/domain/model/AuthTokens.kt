package com.janusleaf.app.domain.model

/**
 * Domain model representing authentication tokens.
 */
data class AuthTokens(
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String,
    val expiresIn: Long
)

/**
 * Domain model for refresh token response (only access token).
 */
data class RefreshedToken(
    val accessToken: String,
    val tokenType: String,
    val expiresIn: Long
)
