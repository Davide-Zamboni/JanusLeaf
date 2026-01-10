package com.janusleaf.service

import com.janusleaf.dto.*
import com.janusleaf.exception.InvalidCredentialsException
import com.janusleaf.exception.InvalidTokenException
import com.janusleaf.exception.UserAlreadyExistsException
import com.janusleaf.exception.UserNotFoundException
import com.janusleaf.model.User
import com.janusleaf.repository.UserRepository
import com.janusleaf.security.JwtTokenProvider
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
class AuthService(
    private val userRepository: UserRepository,
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

    fun login(request: LoginRequest): AuthResponse {
        val user = userRepository.findByEmail(request.email.lowercase())
            ?: throw InvalidCredentialsException()

        if (!passwordEncoder.matches(request.password, user.passwordHash)) {
            throw InvalidCredentialsException()
        }

        return createAuthResponse(user)
    }

    fun refreshToken(request: RefreshTokenRequest): TokenRefreshResponse {
        val token = request.refreshToken

        if (!jwtTokenProvider.validateToken(token)) {
            throw InvalidTokenException("Invalid or expired refresh token")
        }

        val tokenType = jwtTokenProvider.getTokenType(token)
        if (tokenType != JwtTokenProvider.TokenType.REFRESH) {
            throw InvalidTokenException("Invalid token type. Expected refresh token")
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

        return MessageResponse("Password changed successfully")
    }

    private fun createAuthResponse(user: User): AuthResponse {
        val accessToken = jwtTokenProvider.generateAccessToken(user.id, user.email)
        val refreshToken = jwtTokenProvider.generateRefreshToken(user.id, user.email)

        return AuthResponse(
            accessToken = accessToken,
            refreshToken = refreshToken,
            expiresIn = jwtTokenProvider.getAccessTokenExpirationMs() / 1000,
            user = user.toResponse()
        )
    }

    private fun User.toResponse() = UserResponse(
        id = id,
        email = email,
        username = username,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}
