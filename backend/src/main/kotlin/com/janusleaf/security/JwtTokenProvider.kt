package com.janusleaf.security

import io.jsonwebtoken.*
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.*
import javax.crypto.SecretKey

@Component
class JwtTokenProvider(
    @Value("\${jwt.secret}")
    private val jwtSecret: String,

    @Value("\${jwt.access-token-expiration}")
    private val accessTokenExpiration: Long,

    @Value("\${jwt.refresh-token-expiration}")
    private val refreshTokenExpiration: Long
) {
    private val key: SecretKey by lazy {
        Keys.hmacShaKeyFor(jwtSecret.toByteArray())
    }

    fun generateAccessToken(userId: UUID, email: String): String {
        return generateToken(userId, email, accessTokenExpiration, TokenType.ACCESS)
    }

    fun generateRefreshToken(userId: UUID, email: String): String {
        return generateToken(userId, email, refreshTokenExpiration, TokenType.REFRESH)
    }

    private fun generateToken(userId: UUID, email: String, expiration: Long, type: TokenType): String {
        val now = Date()
        val expiryDate = Date(now.time + expiration)

        return Jwts.builder()
            .id(UUID.randomUUID().toString()) // Unique token ID to prevent duplicates
            .subject(userId.toString())
            .claim("email", email)
            .claim("type", type.name)
            .issuedAt(now)
            .expiration(expiryDate)
            .signWith(key)
            .compact()
    }

    fun validateToken(token: String): Boolean {
        return try {
            parseToken(token)
            true
        } catch (e: JwtException) {
            false
        } catch (e: IllegalArgumentException) {
            false
        }
    }

    fun getUserIdFromToken(token: String): UUID {
        val claims = parseToken(token)
        return UUID.fromString(claims.subject)
    }

    fun getEmailFromToken(token: String): String {
        val claims = parseToken(token)
        return claims["email"] as String
    }

    fun getTokenType(token: String): TokenType {
        val claims = parseToken(token)
        return TokenType.valueOf(claims["type"] as String)
    }

    fun getExpirationFromToken(token: String): Date {
        val claims = parseToken(token)
        return claims.expiration
    }

    fun getAccessTokenExpirationMs(): Long = accessTokenExpiration

    private fun parseToken(token: String): Claims {
        return Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .payload
    }

    enum class TokenType {
        ACCESS, REFRESH
    }
}
