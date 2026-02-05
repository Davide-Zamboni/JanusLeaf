package com.janusleaf.app.data.remote.dto

import com.janusleaf.app.domain.model.AuthTokens
import com.janusleaf.app.domain.model.AuthenticatedUser
import com.janusleaf.app.domain.model.RefreshedToken
import com.janusleaf.app.domain.model.User
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

// ==================== Request DTOs ====================

@Serializable
data class RegisterRequestDto(
    val email: String,
    val username: String,
    val password: String
)

@Serializable
data class LoginRequestDto(
    val email: String,
    val password: String
)

@Serializable
data class RefreshTokenRequestDto(
    val refreshToken: String
)

@Serializable
data class ChangePasswordRequestDto(
    val currentPassword: String,
    val newPassword: String
)

@Serializable
data class UpdateProfileRequestDto(
    val username: String
)

@Serializable
data class LogoutRequestDto(
    val refreshToken: String
)

// ==================== Response DTOs ====================

@Serializable
data class UserResponseDto(
    val id: String,
    val email: String,
    val username: String,
    val createdAt: Instant,
    val updatedAt: Instant
) {
    fun toDomain(): User = User(
        id = id,
        email = email,
        username = username,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

@Serializable
data class AuthResponseDto(
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String,
    val expiresIn: Long,
    val user: UserResponseDto
) {
    fun toDomain(): AuthenticatedUser = AuthenticatedUser(
        user = user.toDomain(),
        tokens = AuthTokens(
            accessToken = accessToken,
            refreshToken = refreshToken,
            tokenType = tokenType,
            expiresIn = expiresIn
        )
    )
}

@Serializable
data class TokenRefreshResponseDto(
    val accessToken: String,
    val tokenType: String,
    val expiresIn: Long
) {
    fun toDomain(): RefreshedToken = RefreshedToken(
        accessToken = accessToken,
        tokenType = tokenType,
        expiresIn = expiresIn
    )
}

@Serializable
data class MessageResponseDto(
    val message: String
)

@Serializable
data class ErrorResponseDto(
    val message: String? = null,
    val error: String? = null,
    val status: Int? = null
)
