package com.janusleaf.service

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.ObjectMapper
import com.janusleaf.config.OpenRouterProperties
import com.janusleaf.model.InspirationalQuote
import com.janusleaf.repository.InspirationalQuoteRepository
import com.janusleaf.repository.JournalEntryRepository
import com.janusleaf.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.HttpStatus
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import java.time.Instant
import java.util.*

/**
 * Service for AI-powered inspirational quote generation.
 * 
 * Generates personalized inspirational quotes based on user's last 20 journal entries.
 * Quotes include 4 thematic tags extracted from the journals.
 * 
 * Regeneration triggers:
 * - When user creates a new journal entry
 * - Once per day (24 hours since last generation)
 * - When user has no quote yet
 * 
 * Supports fallback API when primary API is rate limited.
 */
@Service
class InspirationalQuoteService(
    private val openRouterWebClient: WebClient,
    private val fallbackWebClient: WebClient,
    private val openRouterProperties: OpenRouterProperties,
    private val inspirationalQuoteRepository: InspirationalQuoteRepository,
    private val journalEntryRepository: JournalEntryRepository,
    private val userRepository: UserRepository,
    private val objectMapper: ObjectMapper
) {
    private val logger = LoggerFactory.getLogger(InspirationalQuoteService::class.java)

    companion object {
        private const val MAX_JOURNAL_ENTRIES = 20
        private const val MIN_ENTRIES_FOR_QUOTE = 1

        private val QUOTE_GENERATION_PROMPT = """
            You are a thoughtful and empathetic life coach. Based on the following journal entries from a user, 
            generate a personalized, meaningful inspirational quote that resonates with their experiences, 
            emotions, and journey.
            
            The quote should:
            - Be original and personalized (not a famous quote)
            - Reflect themes and emotions from their journals
            - Be encouraging and uplifting
            - Be 1-3 sentences long
            
            Also identify 4 key thematic tags that represent recurring themes in their journals.
            Tags should be single words or short phrases (max 2 words).
            
            Journal entries:
            ---
            %s
            ---
            
            Respond with ONLY a JSON object in this exact format (no markdown, no explanation):
            {"quote": "Your inspirational quote here", "tags": ["tag1", "tag2", "tag3", "tag4"]}
        """.trimIndent()

        private val DEFAULT_QUOTE_PROMPT = """
            Generate a universal, uplifting inspirational quote for someone starting their journaling journey.
            The quote should encourage self-reflection and personal growth.
            
            Also provide 4 general positive tags related to personal growth and journaling.
            
            Respond with ONLY a JSON object in this exact format (no markdown, no explanation):
            {"quote": "Your inspirational quote here", "tags": ["tag1", "tag2", "tag3", "tag4"]}
        """.trimIndent()
    }

    /**
     * Get the inspirational quote for a user.
     * Returns null if user has no quote yet.
     */
    @Transactional(readOnly = true)
    fun getQuoteForUser(userId: UUID): InspirationalQuote? {
        return inspirationalQuoteRepository.findByUserId(userId)
    }

    /**
     * Mark user's quote for regeneration.
     * Called when user creates a new journal entry.
     */
    @Transactional
    fun markForRegeneration(userId: UUID) {
        val updated = inspirationalQuoteRepository.markForRegeneration(userId)
        if (updated > 0) {
            logger.debug("Marked inspirational quote for user $userId for regeneration")
        }
    }

    /**
     * Scheduled job to process quote generation/regeneration.
     * Runs every 30 seconds to check for quotes that need to be generated or updated.
     * Processes only ONE user per invocation to respect rate limits.
     */
    @Scheduled(cron = "\${jobs.inspirational-quote.cron:*/30 * * * * *}")
    @Transactional
    fun processQuoteGeneration() {
        logger.info("Starting inspirational quote generation job")
        
        // Skip if no API key configured
        if (openRouterProperties.apiKey.isBlank()) {
            logger.warn("OpenRouter API key not configured - skipping quote generation")
            return
        }

        // 1. Generate quotes for users who don't have one yet (process ONE at a time)
        val usersWithoutQuotes = inspirationalQuoteRepository.findUserIdsWithoutQuotes()
        if (usersWithoutQuotes.isNotEmpty()) {
            logger.info("Found ${usersWithoutQuotes.size} users without quotes")
            val userId = usersWithoutQuotes.first()
            try {
                generateQuoteForUser(userId)
                return // Process only one per job invocation
            } catch (e: Exception) {
                logger.error("Error generating initial quote for user $userId", e)
            }
        }

        // 2. Regenerate quotes that need updating (flagged or older than 24 hours) - process ONE at a time
        val cutoffTime = Instant.now().minusSeconds(24 * 60 * 60)
        val quotesNeedingRegeneration = inspirationalQuoteRepository.findQuotesNeedingRegeneration(cutoffTime)
        
        if (quotesNeedingRegeneration.isNotEmpty()) {
            logger.info("Found ${quotesNeedingRegeneration.size} quotes needing regeneration")
            val quote = quotesNeedingRegeneration.first()
            try {
                regenerateQuote(quote)
            } catch (e: Exception) {
                logger.error("Error regenerating quote for user ${quote.user.id}", e)
            }
        }
    }

    /**
     * Generate a new quote for a user who doesn't have one yet.
     */
    private fun generateQuoteForUser(userId: UUID) {
        logger.info("Generating initial quote for user $userId")
        
        val journalContent = getJournalContentForUser(userId)
        var result = callApiForQuote(journalContent, useFallback = false)
        
        // If rate limited and fallback is enabled, try the fallback API
        if (result is QuoteApiResult.RateLimited && openRouterProperties.fallback.enabled) {
            logger.info("Primary API rate limited, trying fallback API for user $userId")
            result = callApiForQuote(journalContent, useFallback = true)
        }
        
        when (result) {
            is QuoteApiResult.Success -> {
                val user = userRepository.getReferenceById(userId)
                val quote = InspirationalQuote(
                    user = user,
                    quote = result.data.quote,
                    tags = result.data.tags.toTypedArray(),
                    needsRegeneration = false,
                    lastGeneratedAt = Instant.now()
                )
                inspirationalQuoteRepository.save(quote)
                logger.info("Created inspirational quote for user $userId")
            }
            is QuoteApiResult.RateLimited -> {
                logger.warn("Both APIs rate limited for user $userId - will retry later")
            }
            is QuoteApiResult.Failure -> {
                logger.warn("Failed to generate quote for user $userId - will retry later")
            }
        }
    }

    /**
     * Regenerate an existing quote.
     */
    private fun regenerateQuote(quote: InspirationalQuote) {
        val userId = quote.user.id
        logger.info("Regenerating quote for user $userId")
        
        val journalContent = getJournalContentForUser(userId)
        var result = callApiForQuote(journalContent, useFallback = false)
        
        // If rate limited and fallback is enabled, try the fallback API
        if (result is QuoteApiResult.RateLimited && openRouterProperties.fallback.enabled) {
            logger.info("Primary API rate limited, trying fallback API for user $userId")
            result = callApiForQuote(journalContent, useFallback = true)
        }
        
        when (result) {
            is QuoteApiResult.Success -> {
                quote.quote = result.data.quote
                quote.tags = result.data.tags.toTypedArray()
                quote.needsRegeneration = false
                quote.lastGeneratedAt = Instant.now()
                inspirationalQuoteRepository.save(quote)
                logger.info("Regenerated inspirational quote for user $userId")
            }
            is QuoteApiResult.RateLimited -> {
                logger.warn("Both APIs rate limited for user $userId - will retry later")
            }
            is QuoteApiResult.Failure -> {
                logger.warn("Failed to regenerate quote for user $userId - will retry later")
            }
        }
    }

    /**
     * Get journal content for the last N entries of a user.
     * Returns null if user has insufficient entries.
     */
    private fun getJournalContentForUser(userId: UUID): String? {
        val pageable = PageRequest.of(0, MAX_JOURNAL_ENTRIES, Sort.by(Sort.Direction.DESC, "entryDate"))
        val entries = journalEntryRepository.findByUserId(userId, pageable)
        
        if (entries.isEmpty) {
            logger.debug("User $userId has no journal entries")
            return null
        }

        return entries.content
            .filter { it.body.isNotBlank() }
            .mapIndexed { index, entry -> 
                "Entry ${index + 1} (${entry.entryDate}):\n${entry.title}\n${entry.body}"
            }
            .joinToString("\n\n---\n\n")
            .takeIf { it.isNotBlank() }
    }

    /**
     * Call API (primary or fallback) to generate quote and tags.
     */
    private fun callApiForQuote(journalContent: String?, useFallback: Boolean): QuoteApiResult {
        val prompt = if (journalContent.isNullOrBlank()) {
            DEFAULT_QUOTE_PROMPT
        } else {
            QUOTE_GENERATION_PROMPT.format(journalContent)
        }

        val webClient = if (useFallback) fallbackWebClient else openRouterWebClient
        val model = if (useFallback) openRouterProperties.fallback.model else openRouterProperties.model
        val apiName = if (useFallback) "Fallback" else "OpenRouter"

        val request = OpenRouterRequest(
            model = model,
            messages = listOf(ChatMessage(role = "user", content = prompt)),
            maxTokens = 500,
            temperature = 0.7 // Slightly creative for inspirational content
        )

        return try {
            val response = webClient.post()
                .uri("/chat/completions")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(OpenRouterResponse::class.java)
                .block()

            val content = response?.choices?.firstOrNull()?.message?.content?.trim()
            
            if (content != null) {
                val parsed = parseQuoteResponse(content)
                if (parsed != null) {
                    logger.info("$apiName API returned quote successfully")
                    QuoteApiResult.Success(parsed)
                } else {
                    QuoteApiResult.Failure
                }
            } else {
                logger.warn("$apiName API returned empty response")
                QuoteApiResult.Failure
            }
        } catch (e: WebClientResponseException) {
            logger.error("$apiName API call failed with status ${e.statusCode}", e)
            if (e.statusCode == HttpStatus.TOO_MANY_REQUESTS) {
                QuoteApiResult.RateLimited
            } else {
                QuoteApiResult.Failure
            }
        } catch (e: Exception) {
            logger.error("$apiName API call failed for quote generation", e)
            QuoteApiResult.Failure
        }
    }

    /**
     * Parse the JSON response from the AI.
     */
    private fun parseQuoteResponse(content: String): QuoteGenerationResult? {
        return try {
            // Extract JSON from the response - handles various formats:
            // - Raw JSON: {"quote": "...", "tags": [...]}
            // - Markdown: ```json\n{"quote": "...", "tags": [...]}\n```
            // - With prefix: :\n```json\n{"quote": "...", "tags": [...]}\n```
            val jsonRegex = """\{[^{}]*"quote"\s*:\s*"[^"]*"[^{}]*"tags"\s*:\s*\[[^\]]*\][^{}]*\}""".toRegex()
            val cleanContent = jsonRegex.find(content)?.value
                ?: content
                    .replace(Regex("^[^{]*"), "") // Remove everything before first {
                    .replace(Regex("[^}]*$"), "") // Remove everything after last }
                    .trim()
            
            if (cleanContent.isBlank()) {
                logger.warn("Could not extract JSON from response: $content")
                return null
            }
            
            val result = objectMapper.readValue(cleanContent, QuoteGenerationResult::class.java)
            
            // Validate the result
            if (result.quote.isBlank()) {
                logger.warn("Received empty quote from AI")
                return null
            }
            
            // Ensure exactly 4 tags
            val tags = result.tags
                .filter { it.isNotBlank() }
                .take(4)
                .toMutableList()
            
            // Pad with default tags if needed
            while (tags.size < 4) {
                tags.add(listOf("growth", "reflection", "journey", "mindfulness")[tags.size])
            }
            
            QuoteGenerationResult(result.quote, tags)
        } catch (e: Exception) {
            logger.error("Failed to parse quote response: $content", e)
            null
        }
    }
}

/**
 * Result of a quote generation API call.
 */
sealed class QuoteApiResult {
    data class Success(val data: QuoteGenerationResult) : QuoteApiResult()
    data object RateLimited : QuoteApiResult()
    data object Failure : QuoteApiResult()
}

/**
 * Result from AI quote generation.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class QuoteGenerationResult(
    val quote: String,
    val tags: List<String>
)
