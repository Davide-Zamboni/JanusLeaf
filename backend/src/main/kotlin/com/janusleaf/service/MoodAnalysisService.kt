package com.janusleaf.service

import com.fasterxml.jackson.annotation.JsonProperty
import com.janusleaf.config.OpenRouterProperties
import com.janusleaf.model.MoodAnalysisQueue
import com.janusleaf.repository.JournalEntryRepository
import com.janusleaf.repository.MoodAnalysisQueueRepository
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import java.time.Instant
import java.util.*
import kotlin.math.pow

/**
 * Service for AI-powered mood analysis of journal entries.
 * 
 * Uses a database-backed queue for reliable, debounced processing:
 * - When body is edited, a queue entry is created/updated with scheduledFor = now + delay
 * - Each subsequent edit resets scheduledFor (debouncing)
 * - A scheduled job polls for ready entries and processes them
 * - After successful analysis, the queue entry is deleted
 * 
 * Benefits over in-memory scheduling:
 * - Survives server restarts
 * - Works with multiple server instances
 * - Observable (can query pending analyses)
 */
@Service
@EnableScheduling
class MoodAnalysisService(
    private val openRouterWebClient: WebClient,
    private val fallbackWebClient: WebClient,
    private val openRouterProperties: OpenRouterProperties,
    private val journalEntryRepository: JournalEntryRepository,
    private val moodAnalysisQueueRepository: MoodAnalysisQueueRepository
) {
    private val logger = LoggerFactory.getLogger(MoodAnalysisService::class.java)

    companion object {
        private const val MIN_BODY_LENGTH = 10 // Don't analyze very short entries
        
        private val MOOD_ANALYSIS_PROMPT = """
            You are a mood analysis assistant. Analyze the following journal entry and rate the overall emotional mood on a scale from 1 to 10.
            
            Scale guide:
            1-2: Very negative (depressed, hopeless, extremely sad)
            3-4: Negative (sad, frustrated, anxious, stressed)
            5-6: Neutral to slightly negative/positive (mixed feelings, okay)
            7-8: Positive (happy, content, grateful, optimistic)
            9-10: Very positive (joyful, excited, deeply grateful, elated)
            
            IMPORTANT: Respond with ONLY a single integer from 1 to 10. No explanation, no text, just the number.
            
            Journal entry:
            ---
            %s
            ---
            
            Mood score (1-10):
        """.trimIndent()
    }

    /**
     * Queue a mood analysis for a journal entry.
     * If an entry already exists, update it (reset the debounce timer).
     * 
     * @param entryId The journal entry ID to analyze
     * @param body The current body text of the entry
     */
    @Transactional
    fun queueMoodAnalysis(entryId: UUID, body: String) {
        // Skip analysis for very short or empty entries
        if (body.length < MIN_BODY_LENGTH) {
            logger.debug("Skipping mood analysis for entry $entryId - body too short (${body.length} chars)")
            // Remove any existing queue entry since body is now too short
            moodAnalysisQueueRepository.deleteByJournalEntryId(entryId)
            return
        }

        val scheduledFor = Instant.now().plusMillis(openRouterProperties.debounceDelayMs)
        
        // Check if there's already a pending analysis for this entry
        val existingEntry = moodAnalysisQueueRepository.findByJournalEntryId(entryId)
        
        if (existingEntry != null) {
            // Update existing entry - reset debounce timer and update body snapshot
            existingEntry.bodySnapshot = body
            existingEntry.scheduledFor = scheduledFor
            moodAnalysisQueueRepository.save(existingEntry)
            logger.debug("Updated pending mood analysis for entry $entryId, rescheduled for $scheduledFor")
        } else {
            // Create new queue entry
            val queueEntry = MoodAnalysisQueue(
                journalEntryId = entryId,
                bodySnapshot = body,
                scheduledFor = scheduledFor
            )
            moodAnalysisQueueRepository.save(queueEntry)
            logger.debug("Queued new mood analysis for entry $entryId, scheduled for $scheduledFor")
        }
    }

    /**
     * Cancel any pending mood analysis for an entry.
     * Called when a journal entry is deleted.
     */
    @Transactional
    fun cancelPendingAnalysis(entryId: UUID) {
        val deleted = moodAnalysisQueueRepository.deleteByJournalEntryId(entryId)
        if (deleted > 0) {
            logger.debug("Cancelled pending mood analysis for entry $entryId")
        }
    }

    /**
     * Scheduled job to process ready queue entries.
     * Runs on a cron schedule (default: every 3 seconds) to pick up entries whose debounce period has passed.
     * Processes only ONE entry per invocation to respect rate limits.
     */
    @Scheduled(cron = "\${jobs.mood-analysis.cron:*/3 * * * * *}")
    @Transactional
    fun processQueue() {
        logger.info("Starting mood analysis queue processing")
        // Skip if no API key configured
        if (openRouterProperties.apiKey.isBlank()) {
            logger.warn("OpenRouter API key not configured - skipping mood analysis processing")
            return
        }

        val readyEntries = moodAnalysisQueueRepository.findReadyToProcess(Instant.now())
        
        if (readyEntries.isEmpty()) {
            return
        }

        logger.info("Processing ${readyEntries.size} pending mood analyses")

        // Process only ONE entry per invocation to respect rate limits
        // The scheduler runs frequently enough to process the queue over time
        val queueEntry = readyEntries.first()
        try {
            processQueueEntry(queueEntry)
        } catch (e: Exception) {
            logger.error("Error processing mood analysis for entry ${queueEntry.journalEntryId}", e)
            // Don't delete - will retry on next poll
        }
    }

    /**
     * Process a single queue entry.
     */
    private fun processQueueEntry(queueEntry: MoodAnalysisQueue) {
        val entryId = queueEntry.journalEntryId
        logger.info("Analyzing mood for entry $entryId (attempt ${queueEntry.retryCount + 1})")

        // Call OpenRouter API
        var result = callOpenRouterForMoodScore(queueEntry.bodySnapshot)

        // If rate limited and fallback is enabled, try the fallback API
        if (result is MoodAnalysisResult.RateLimited && openRouterProperties.fallback.enabled) {
            logger.info("Primary API rate limited, trying fallback API for entry $entryId")
            result = callFallbackApiForMoodScore(queueEntry.bodySnapshot)
        }

        when (result) {
            is MoodAnalysisResult.Success -> {
                // Update the journal entry with the mood score
                journalEntryRepository.findById(entryId).ifPresent { entry ->
                    entry.moodScore = result.score
                    journalEntryRepository.save(entry)
                    logger.info("Updated mood score for entry $entryId: ${result.score}")
                }
                // Delete the queue entry - analysis complete
                moodAnalysisQueueRepository.delete(queueEntry)
            }
            is MoodAnalysisResult.RateLimited -> {
                handleRateLimitError(queueEntry)
            }
            is MoodAnalysisResult.Failure -> {
                logger.warn("Failed to get mood score for entry $entryId - will retry")
                // Increment retry count and reschedule with smaller backoff
                queueEntry.retryCount++
                if (queueEntry.retryCount >= openRouterProperties.maxRetries) {
                    logger.error("Max retries exceeded for entry $entryId - giving up")
                    moodAnalysisQueueRepository.delete(queueEntry)
                } else {
                    val backoffMs = openRouterProperties.debounceDelayMs * (queueEntry.retryCount + 1)
                    queueEntry.scheduledFor = Instant.now().plusMillis(backoffMs)
                    moodAnalysisQueueRepository.save(queueEntry)
                }
            }
        }
    }

    /**
     * Handle rate limit (429) error with exponential backoff.
     */
    private fun handleRateLimitError(queueEntry: MoodAnalysisQueue) {
        queueEntry.retryCount++
        
        if (queueEntry.retryCount >= openRouterProperties.maxRetries) {
            logger.error("Max retries exceeded for entry ${queueEntry.journalEntryId} due to rate limiting - giving up")
            moodAnalysisQueueRepository.delete(queueEntry)
            return
        }
        
        // Exponential backoff: base * 2^retryCount
        // e.g., with base=10s: 10s, 20s, 40s, 80s, 160s
        val backoffMs = openRouterProperties.rateLimitBackoffBaseMs * 2.0.pow(queueEntry.retryCount - 1).toLong()
        queueEntry.scheduledFor = Instant.now().plusMillis(backoffMs)
        moodAnalysisQueueRepository.save(queueEntry)
        
        logger.warn(
            "Rate limited for entry ${queueEntry.journalEntryId} - " +
            "retry ${queueEntry.retryCount}/${openRouterProperties.maxRetries}, " +
            "backing off for ${backoffMs / 1000}s"
        )
    }

    /**
     * Call OpenRouter API to get mood score.
     */
    private fun callOpenRouterForMoodScore(body: String): MoodAnalysisResult {
        val request = OpenRouterRequest(
            model = openRouterProperties.model,
            messages = listOf(
                ChatMessage(role = "user", content = MOOD_ANALYSIS_PROMPT.format(body))
            ),
            maxTokens = 5, // We only need a single digit
            temperature = 0.1 // Low temperature for consistent results
        )

        return try {
            val response = openRouterWebClient.post()
                .uri("/chat/completions")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(OpenRouterResponse::class.java)
                .block()

            val content = response?.choices?.firstOrNull()?.message?.content?.trim()
            
            // Parse the response - should be just a number
            val score = content?.toIntOrNull()?.takeIf { it in 1..10 }
            if (score != null) {
                MoodAnalysisResult.Success(score)
            } else {
                logger.warn("Invalid mood score response: $content")
                MoodAnalysisResult.Failure
            }
        } catch (e: WebClientResponseException) {
            logger.error("OpenRouter API call failed with status ${e.statusCode}", e)
            if (e.statusCode == HttpStatus.TOO_MANY_REQUESTS) {
                MoodAnalysisResult.RateLimited
            } else {
                MoodAnalysisResult.Failure
            }
        } catch (e: Exception) {
            logger.error("OpenRouter API call failed", e)
            MoodAnalysisResult.Failure
        }
    }

    /**
     * Call fallback API (ChatAnywhere - OpenAI compatible) to get mood score.
     * Used when the primary API is rate limited.
     * See: https://github.com/chatanywhere/GPT_API_free
     */
    private fun callFallbackApiForMoodScore(body: String): MoodAnalysisResult {
        val request = OpenRouterRequest(
            model = openRouterProperties.fallback.model,
            messages = listOf(
                ChatMessage(role = "user", content = MOOD_ANALYSIS_PROMPT.format(body))
            ),
            maxTokens = 5,
            temperature = 0.1
        )

        return try {
            val response = fallbackWebClient.post()
                .uri("/chat/completions")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(OpenRouterResponse::class.java)
                .block()

            val content = response?.choices?.firstOrNull()?.message?.content?.trim()
            
            // Parse the response - should be just a number
            val score = content?.toIntOrNull()?.takeIf { it in 1..10 }
            if (score != null) {
                logger.info("Fallback API returned mood score: $score")
                MoodAnalysisResult.Success(score)
            } else {
                logger.warn("Invalid mood score response from fallback API: $content")
                MoodAnalysisResult.Failure
            }
        } catch (e: WebClientResponseException) {
            logger.error("Fallback API call failed with status ${e.statusCode}", e)
            if (e.statusCode == HttpStatus.TOO_MANY_REQUESTS) {
                MoodAnalysisResult.RateLimited
            } else {
                MoodAnalysisResult.Failure
            }
        } catch (e: Exception) {
            logger.error("Fallback API call failed", e)
            MoodAnalysisResult.Failure
        }
    }
}

/**
 * Result of a mood analysis API call.
 */
sealed class MoodAnalysisResult {
    data class Success(val score: Int) : MoodAnalysisResult()
    data object RateLimited : MoodAnalysisResult()
    data object Failure : MoodAnalysisResult()
}

// ==================== OpenRouter API DTOs ====================

data class OpenRouterRequest(
    val model: String,
    val messages: List<ChatMessage>,
    @JsonProperty("max_tokens")
    val maxTokens: Int = 5,
    val temperature: Double = 0.1
)

data class ChatMessage(
    val role: String,
    val content: String
)

data class OpenRouterResponse(
    val id: String? = null,
    val choices: List<Choice> = emptyList()
)

data class Choice(
    val index: Int = 0,
    val message: ResponseMessage? = null
)

data class ResponseMessage(
    val role: String? = null,
    val content: String? = null
)
