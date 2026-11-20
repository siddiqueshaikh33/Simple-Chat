package com.example.chatapp.model;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Domain model representing a chat message stored in Redis.
 * Serialized as a clean JSON string with exactly three fields (no @class or extra metadata).
 *
 * @param participant username of the sender
 * @param message     the message content
 * @param timestamp   UTC ISO-8601 timestamp truncated to seconds (e.g. 2024-01-01T10:00:00Z)
 */
@Schema(description = "Chat message representation")
public record ChatMessage(
        @Schema(description = "Username of the sender", example = "guest_user")
        String participant,

        @Schema(description = "Message body text", example = "Hello, everyone!")
        String message,

        @Schema(description = "UTC ISO-8601 timestamp truncated to seconds", example = "2024-01-01T10:00:00Z")
        String timestamp
) {}
