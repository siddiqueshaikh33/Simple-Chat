package com.example.chatapp.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Health check status response payload.
 *
 * @param status    Overall health status ("UP" or "DOWN")
 * @param redis     Redis connectivity status ("UP" or "DOWN")
 * @param timestamp ISO-8601 UTC timestamp of the health check
 * @param error     Error message if degraded, or null if healthy
 */
@Schema(description = "Health status payload for application and infrastructure checks")
public record HealthResponse(
        @Schema(description = "Overall health status", example = "UP")
        String status,

        @Schema(description = "Redis connectivity status", example = "UP")
        String redis,

        @Schema(description = "UTC timestamp of the check", example = "2026-11-21T03:00:00Z")
        String timestamp,

        @Schema(description = "Optional error details if degraded", example = "null")
        String error
) {
    public static HealthResponse up(String timestamp) {
        return new HealthResponse("UP", "UP", timestamp, null);
    }

    public static HealthResponse down(String timestamp, String error) {
        return new HealthResponse("DOWN", "DOWN", timestamp, error);
    }
}
