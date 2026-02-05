package com.janusleaf.app.data.remote.dto

import com.janusleaf.app.domain.model.InspirationalQuote
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

// ==================== Response DTOs ====================

@Serializable
data class InspirationalQuoteResponseDto(
    val id: String,
    val quote: String,
    val tags: List<String>,
    val generatedAt: Instant,
    val updatedAt: Instant
) {
    fun toDomain(): InspirationalQuote = InspirationalQuote(
        id = id,
        quote = quote,
        tags = tags,
        generatedAt = generatedAt,
        updatedAt = updatedAt
    )
}
