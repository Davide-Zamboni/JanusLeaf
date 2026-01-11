package com.janusleaf.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration

/**
 * Configuration for mood analysis job scheduling.
 */
@ConfigurationProperties(prefix = "jobs.mood-analysis")
data class MoodAnalysisJobProperties(
    /** Cron expression for queue polling. Default: every 3 seconds */
    val cron: String = "*/3 * * * * *"
)

/**
 * Configuration for inspirational quote generation job scheduling.
 */
@ConfigurationProperties(prefix = "jobs.inspirational-quote")
data class InspirationalQuoteJobProperties(
    /** Cron expression for quote generation polling. Default: every 30 seconds */
    val cron: String = "*/30 * * * * *"
)

/**
 * Root configuration for all scheduled jobs.
 */
@Configuration
@EnableConfigurationProperties(MoodAnalysisJobProperties::class, InspirationalQuoteJobProperties::class)
class JobsConfig
