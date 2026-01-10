package com.janusleaf.dto

import java.time.Instant
import java.util.*

// ==================== Response DTOs ====================

/**
 * Response containing the user's inspirational quote with tags.
 */
data class InspirationalQuoteResponse(
    val id: UUID,
    val quote: String,
    val tags: List<String>,
    val generatedAt: Instant,
    val updatedAt: Instant
)

/**
 * Response when user has no quote yet.
 */
data class NoQuoteResponse(
    val message: String = "No inspirational quote generated yet. One will be created shortly based on your journal entries."
)
