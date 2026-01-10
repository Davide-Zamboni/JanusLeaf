package com.janusleaf.repository

import com.janusleaf.model.RefreshToken
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.*

@Repository
interface RefreshTokenRepository : JpaRepository<RefreshToken, UUID> {
    
    fun findByTokenHash(tokenHash: String): RefreshToken?
    
    @Modifying
    @Query("UPDATE RefreshToken r SET r.revokedAt = :revokedAt WHERE r.user.id = :userId AND r.revokedAt IS NULL")
    fun revokeAllByUserId(userId: UUID, revokedAt: Instant = Instant.now()): Int
    
    @Modifying
    @Query("DELETE FROM RefreshToken r WHERE r.expiresAt < :now OR r.revokedAt IS NOT NULL")
    fun deleteExpiredAndRevoked(now: Instant = Instant.now()): Int
    
    fun countByUserIdAndRevokedAtIsNull(userId: UUID): Long
}
