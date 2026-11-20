package com.example.chatapp.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request payload for sending a chat message to a room.
 *
 * @param participant username of the sender (not blank, max 50 characters)
 * @param message     the message content (not blank, max 1000 characters)
 */
@Schema(description = "Request payload to send a chat message")
public record SendMessageRequest(
        @Schema(description = "Username of the participant sending the message", example = "guest_user")
        @NotBlank(message = "participant must not be blank")
        @Size(max = 50, message = "participant must not exceed 50 characters")
        String participant,

        @Schema(description = "Text content of the message", example = "Hello, everyone!")
        @NotBlank(message = "message must not be blank")
        @Size(max = 1000, message = "message must not exceed 1000 characters")
        String message
) {}
