package com.janusleaf.security

import org.springframework.security.core.annotation.AuthenticationPrincipal

/**
 * Custom annotation to inject the current authenticated user into controller methods.
 * 
 * Usage:
 * ```
 * @GetMapping("/me")
 * fun getCurrentUser(@CurrentUser user: UserPrincipal): UserResponse {
 *     // user.id and user.email are available
 * }
 * ```
 */
@Target(AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.TYPE)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
@AuthenticationPrincipal
annotation class CurrentUser
