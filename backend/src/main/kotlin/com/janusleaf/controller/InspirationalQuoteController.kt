package com.janusleaf.controller

import com.janusleaf.dto.InspirationalQuoteResponse
import com.janusleaf.dto.NoQuoteResponse
import com.janusleaf.security.CurrentUser
import com.janusleaf.security.UserPrincipal
import com.janusleaf.service.InspirationalQuoteService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/inspiration")
class InspirationalQuoteController(
    private val inspirationalQuoteService: InspirationalQuoteService
) {

    /**
     * Get the current inspirational quote for the authenticated user.
     * 
     * The quote is AI-generated based on the user's last 20 journal entries.
     * Includes 4 thematic tags extracted from the journals.
     * 
     * GET /api/inspiration
     * 
     * Response includes:
     * - quote: The inspirational quote text
     * - tags: Array of 4 thematic tags
     * - generatedAt: When the quote was last generated
     * 
     * If no quote exists yet, returns 404 with a message indicating
     * that a quote will be generated shortly.
     */
    @GetMapping
    fun getInspirationalQuote(
        @CurrentUser user: UserPrincipal
    ): ResponseEntity<Any> {
        val quote = inspirationalQuoteService.getQuoteForUser(user.id)
        
        return if (quote != null) {
            ResponseEntity.ok(
                InspirationalQuoteResponse(
                    id = quote.id,
                    quote = quote.quote,
                    tags = quote.tags.toList(),
                    generatedAt = quote.lastGeneratedAt,
                    updatedAt = quote.updatedAt
                )
            )
        } else {
            ResponseEntity.status(404).body(
                NoQuoteResponse()
            )
        }
    }
}
