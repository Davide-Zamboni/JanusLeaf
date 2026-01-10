package com.janusleaf.controller

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

@RestController
@RequestMapping("/api")
class HealthController {

    @GetMapping("/health")
    fun health(): ResponseEntity<HealthResponse> {
        return ResponseEntity.ok(
            HealthResponse(
                status = "UP",
                version = "1.0.0",
                timestamp = Instant.now()
            )
        )
    }
}

data class HealthResponse(
    val status: String,
    val version: String,
    val timestamp: Instant
)
