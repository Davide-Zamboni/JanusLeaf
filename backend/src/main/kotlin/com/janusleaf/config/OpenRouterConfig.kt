package com.janusleaf.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient

@ConfigurationProperties(prefix = "openrouter")
data class OpenRouterProperties(
    val apiKey: String = "",
    val baseUrl: String = "https://openrouter.ai/api/v1",
    val model: String = "google/gemma-3-27b-it:free",
    val debounceDelayMs: Long = 5000,
    /** Delay between consecutive API requests to avoid rate limiting (milliseconds) */
    val requestDelayMs: Long = 2000,
    /** Base delay for exponential backoff on rate limit errors (milliseconds) */
    val rateLimitBackoffBaseMs: Long = 10000,
    /** Maximum number of retries before giving up */
    val maxRetries: Int = 5,
    /** Fallback API configuration (ChatAnywhere - OpenAI compatible) */
    val fallback: FallbackApiProperties = FallbackApiProperties()
)

/**
 * Fallback API configuration using ChatAnywhere (https://github.com/chatanywhere/GPT_API_free)
 * OpenAI-compatible API that can be used when the primary API is rate limited.
 */
data class FallbackApiProperties(
    val enabled: Boolean = false,
    val apiKey: String = "",
    val baseUrl: String = "https://api.chatanywhere.org/v1",
    val model: String = "gpt-4o-mini"
)

@Configuration
@EnableConfigurationProperties(OpenRouterProperties::class)
class OpenRouterConfig(private val properties: OpenRouterProperties) {

    @Bean
    fun openRouterWebClient(): WebClient {
        return WebClient.builder()
            .baseUrl(properties.baseUrl)
            .defaultHeader("Authorization", "Bearer ${properties.apiKey}")
            .defaultHeader("Content-Type", "application/json")
            .defaultHeader("HTTP-Referer", "https://janusleaf.app") // Required by OpenRouter
            .defaultHeader("X-Title", "JanusLeaf Journal")
            .build()
    }

    /**
     * Fallback WebClient using ChatAnywhere API (OpenAI-compatible).
     * See: https://github.com/chatanywhere/GPT_API_free
     */
    @Bean
    fun fallbackWebClient(): WebClient {
        return WebClient.builder()
            .baseUrl(properties.fallback.baseUrl)
            .defaultHeader("Authorization", "Bearer ${properties.fallback.apiKey}")
            .defaultHeader("Content-Type", "application/json")
            .build()
    }
}
