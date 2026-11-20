package com.example.chatapp.controller;

import com.example.chatapp.dto.HealthResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Health check controller providing endpoints for cloud container readiness,
 * cloud health probes (Railway / Kubernetes / Docker), and live status verification.
 */
@RestController
@Tag(name = "Health", description = "Endpoints for application health and Redis connectivity checks")
public class HealthController {

    private static final Logger log = LoggerFactory.getLogger(HealthController.class);

    private final StringRedisTemplate stringRedisTemplate;
    private final Clock clock;

    public HealthController(StringRedisTemplate stringRedisTemplate, Clock clock) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.clock = clock;
    }

    /**
     * Root endpoint providing quick application summary and documentation links.
     * Prevents 404 Whitelabel Error when opening the base deployed URL.
     */
    @GetMapping("/")
    @Operation(summary = "Root status", description = "Returns service information and documentation links.")
    public ResponseEntity<Map<String, Object>> root() {
        return ResponseEntity.ok(Map.of(
                "service", "Simple Chat Backend API",
                "status", "RUNNING",
                "healthCheck", "/health",
                "apiDocs", "/swagger-ui.html",
                "timestamp", Instant.now(clock).toString()
        ));
    }

    /**
     * Checks application health and active Redis connectivity.
     * Mapped to both /health and /api/chatapp/health.
     *
     * @return HTTP 200 with status UP if healthy, or HTTP 503 with status DOWN if Redis is unreachable
     */
    @GetMapping({"/health", "/api/chatapp/health"})
    @Operation(summary = "Health check", description = "Returns the status of the application and verifies active Redis connectivity.")
    @ApiResponse(responseCode = "200", description = "Application and Redis are healthy",
            content = @Content(schema = @Schema(implementation = HealthResponse.class)))
    @ApiResponse(responseCode = "503", description = "Application is running but Redis connection failed",
            content = @Content(schema = @Schema(implementation = HealthResponse.class)))
    public ResponseEntity<HealthResponse> healthCheck() {
        String timestamp = Instant.now(clock).toString();
        try {
            String pong = stringRedisTemplate.execute((RedisCallback<String>) connection -> connection.ping());
            if ("PONG".equalsIgnoreCase(pong)) {
                return ResponseEntity.ok(HealthResponse.up(timestamp));
            } else {
                log.warn("Redis health check returned unexpected response: {}", pong);
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                        .body(HealthResponse.down(timestamp, "Unexpected Redis ping response: " + pong));
            }
        } catch (Exception ex) {
            log.error("Redis health check failed: {}", ex.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(HealthResponse.down(timestamp, "Redis connection failed: " + ex.getMessage()));
        }
    }
}
