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
    val debounceDelayMs: Long = 5000
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
}
