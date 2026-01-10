package com.janusleaf.service

import com.janusleaf.dto.*
import com.janusleaf.exception.InvalidCredentialsException
import com.janusleaf.exception.InvalidTokenException
import com.janusleaf.exception.UserAlreadyExistsException
import com.janusleaf.exception.UserNotFoundException
import com.janusleaf.model.RefreshToken
import com.janusleaf.model.User
import com.janusleaf.repository.RefreshTokenRepository
import com.janusleaf.repository.UserRepository
import com.janusleaf.security.JwtTokenProvider
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.MessageDigest
import java.time.Instant
import java.util.*

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtTokenProvider: JwtTokenProvider
) {

    @Transactional
    fun register(request: RegisterRequest): AuthResponse {
        // Check if email already exists
        if (userRepository.existsByEmail(request.email.lowercase())) {
            throw UserAlreadyExistsException("Email '${request.email}' is already registered")
        }

        // Create new user
        val user = User(
            email = request.email.lowercase().trim(),
            username = request.username.trim(),
            passwordHash = passwordEncoder.encode(request.password)
        )

        val savedUser = userRepository.save(user)

        // Generate tokens
        return createAuthResponse(savedUser)
    }

    @Transactional
    fun login(request: LoginRequest): AuthResponse {
        val user = userRepository.findByEmail(request.email.lowercase())
            ?: throw InvalidCredentialsException()

        if (!passwordEncoder.matches(request.password, user.passwordHash)) {
            throw InvalidCredentialsException()
        }

        return createAuthResponse(user)
    }

    @Transactional
    fun refreshToken(request: RefreshTokenRequest): TokenRefreshResponse {
        val token = request.refreshToken

        // Validate JWT structure and signature
        if (!jwtTokenProvider.validateToken(token)) {
            throw InvalidTokenException("Invalid or expired refresh token")
        }

        val tokenType = jwtTokenProvider.getTokenType(token)
        if (tokenType != JwtTokenProvider.TokenType.REFRESH) {
            throw InvalidTokenException("Invalid token type. Expected refresh token")
        }

        // Check if token exists in database and is valid
        val tokenHash = hashToken(token)
        val storedToken = refreshTokenRepository.findByTokenHash(tokenHash)
            ?: throw InvalidTokenException("Refresh token not found or already revoked")

        if (!storedToken.isValid()) {
            throw InvalidTokenException("Refresh token has been revoked or expired")
        }

        val userId = jwtTokenProvider.getUserIdFromToken(token)
        val email = jwtTokenProvider.getEmailFromToken(token)

        // Verify user still exists
        if (!userRepository.existsById(userId)) {
            throw InvalidTokenException("User no longer exists")
        }

        val newAccessToken = jwtTokenProvider.generateAccessToken(userId, email)

        return TokenRefreshResponse(
            accessToken = newAccessToken,
            expiresIn = jwtTokenProvider.getAccessTokenExpirationMs() / 1000
        )
    }

    @Transactional
    fun logout(request: LogoutRequest) {
        val token = request.refreshToken

        // Validate and revoke the refresh token
        if (jwtTokenProvider.validateToken(token)) {
            val tokenHash = hashToken(token)
            val storedToken = refreshTokenRepository.findByTokenHash(tokenHash)
            storedToken?.revoke()
        }
        // If token is invalid or not found, just ignore - user is effectively logged out
    }

    @Transactional
    fun logoutAll(userId: UUID): Int {
        // Revoke all refresh tokens for the user
        return refreshTokenRepository.revokeAllByUserId(userId)
    }

    fun getCurrentUser(userId: UUID): UserResponse {
        val user = userRepository.findById(userId)
            .orElseThrow { UserNotFoundException() }
        
        return user.toResponse()
    }

    @Transactional
    fun updateProfile(userId: UUID, request: UpdateProfileRequest): UserResponse {
        val user = userRepository.findById(userId)
            .orElseThrow { UserNotFoundException() }

        request.username?.let { 
            user.username = it.trim() 
        }

        val updatedUser = userRepository.save(user)
        return updatedUser.toResponse()
    }

    @Transactional
    fun changePassword(userId: UUID, request: ChangePasswordRequest): MessageResponse {
        val user = userRepository.findById(userId)
            .orElseThrow { UserNotFoundException() }

        if (!passwordEncoder.matches(request.currentPassword, user.passwordHash)) {
            throw InvalidCredentialsException("Current password is incorrect")
        }

        user.passwordHash = passwordEncoder.encode(request.newPassword)
        userRepository.save(user)

        // Revoke all existing refresh tokens for security
        refreshTokenRepository.revokeAllByUserId(userId)

        return MessageResponse("Password changed successfully. Please login again on all devices.")
    }

    @Transactional
    fun createAuthResponse(user: User): AuthResponse {
        val accessToken = jwtTokenProvider.generateAccessToken(user.id, user.email)
        val refreshTokenJwt = jwtTokenProvider.generateRefreshToken(user.id, user.email)

        // Store refresh token in database
        val expiresAt = jwtTokenProvider.getExpirationFromToken(refreshTokenJwt).toInstant()
        val refreshToken = RefreshToken(
            tokenHash = hashToken(refreshTokenJwt),
            user = user,
            expiresAt = expiresAt
        )
        refreshTokenRepository.save(refreshToken)

        return AuthResponse(
            accessToken = accessToken,
            refreshToken = refreshTokenJwt,
            expiresIn = jwtTokenProvider.getAccessTokenExpirationMs() / 1000,
            user = user.toResponse()
        )
    }

    private fun hashToken(token: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(token.toByteArray())
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    private fun User.toResponse() = UserResponse(
        id = id,
        email = email,
        username = username,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}
