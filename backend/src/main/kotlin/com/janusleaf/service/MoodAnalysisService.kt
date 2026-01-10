package com.janusleaf.service

import com.fasterxml.jackson.annotation.JsonProperty
import com.janusleaf.config.OpenRouterProperties
import com.janusleaf.model.MoodAnalysisQueue
import com.janusleaf.repository.JournalEntryRepository
import com.janusleaf.repository.MoodAnalysisQueueRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.reactive.function.client.WebClient
import java.time.Instant
import java.util.*

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

        for (queueEntry in readyEntries) {
            try {
                processQueueEntry(queueEntry)
            } catch (e: Exception) {
                logger.error("Error processing mood analysis for entry ${queueEntry.journalEntryId}", e)
                // Don't delete - will retry on next poll
            }
        }
    }

    /**
     * Process a single queue entry.
     */
    private fun processQueueEntry(queueEntry: MoodAnalysisQueue) {
        val entryId = queueEntry.journalEntryId
        logger.info("Analyzing mood for entry $entryId")

        // Call OpenRouter API
        val moodScore = callOpenRouterForMoodScore(queueEntry.bodySnapshot)

        if (moodScore != null) {
            // Update the journal entry with the mood score
            journalEntryRepository.findById(entryId).ifPresent { entry ->
                entry.moodScore = moodScore
                journalEntryRepository.save(entry)
                logger.info("Updated mood score for entry $entryId: $moodScore")
            }
            
            // Delete the queue entry - analysis complete
            moodAnalysisQueueRepository.delete(queueEntry)
        } else {
            logger.warn("Failed to get mood score for entry $entryId - will retry")
            // Don't delete - will retry on next poll
        }
    }

    /**
     * Call OpenRouter API to get mood score.
     */
    private fun callOpenRouterForMoodScore(body: String): Int? {
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
            content?.toIntOrNull()?.takeIf { it in 1..10 }
        } catch (e: Exception) {
            logger.error("OpenRouter API call failed", e)
            null
        }
    }
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
