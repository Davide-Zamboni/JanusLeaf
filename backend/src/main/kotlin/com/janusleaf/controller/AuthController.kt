package com.janusleaf.controller

import com.janusleaf.dto.*
import com.janusleaf.security.CurrentUser
import com.janusleaf.security.UserPrincipal
import com.janusleaf.service.AuthService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val authService: AuthService
) {

    /**
     * Register a new user account.
     * POST /api/auth/register
     */
    @PostMapping("/register")
    fun register(@Valid @RequestBody request: RegisterRequest): ResponseEntity<AuthResponse> {
        val response = authService.register(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    /**
     * Login with email and password.
     * POST /api/auth/login
     */
    @PostMapping("/login")
    fun login(@Valid @RequestBody request: LoginRequest): ResponseEntity<AuthResponse> {
        val response = authService.login(request)
        return ResponseEntity.ok(response)
    }

    /**
     * Refresh the access token using a refresh token.
     * POST /api/auth/refresh
     */
    @PostMapping("/refresh")
    fun refreshToken(@Valid @RequestBody request: RefreshTokenRequest): ResponseEntity<TokenRefreshResponse> {
        val response = authService.refreshToken(request)
        return ResponseEntity.ok(response)
    }

    /**
     * Get the current authenticated user's profile.
     * GET /api/auth/me
     */
    @GetMapping("/me")
    fun getCurrentUser(@CurrentUser user: UserPrincipal): ResponseEntity<UserResponse> {
        val response = authService.getCurrentUser(user.id)
        return ResponseEntity.ok(response)
    }

    /**
     * Update the current user's profile.
     * PUT /api/auth/me
     */
    @PutMapping("/me")
    fun updateProfile(
        @CurrentUser user: UserPrincipal,
        @Valid @RequestBody request: UpdateProfileRequest
    ): ResponseEntity<UserResponse> {
        val response = authService.updateProfile(user.id, request)
        return ResponseEntity.ok(response)
    }

    /**
     * Change the current user's password.
     * POST /api/auth/change-password
     */
    @PostMapping("/change-password")
    fun changePassword(
        @CurrentUser user: UserPrincipal,
        @Valid @RequestBody request: ChangePasswordRequest
    ): ResponseEntity<MessageResponse> {
        val response = authService.changePassword(user.id, request)
        return ResponseEntity.ok(response)
    }

    /**
     * Logout the current user.
     * POST /api/auth/logout
     * Note: Since we use stateless JWT, this is mainly for client-side token cleanup.
     * In a production app, you might want to implement token blacklisting.
     */
    @PostMapping("/logout")
    fun logout(@CurrentUser user: UserPrincipal): ResponseEntity<MessageResponse> {
        // With stateless JWT, logout is handled client-side by discarding the token
        // In production, you could implement token blacklisting here
        return ResponseEntity.ok(MessageResponse("Logged out successfully"))
    }
}
