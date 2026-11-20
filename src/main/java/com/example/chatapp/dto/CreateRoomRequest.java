package com.example.chatapp.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request payload for creating a new chat room.
 *
 * @param roomName the unique name of the chat room (1-50 alphanumeric, '-' or '_' characters)
 */
@Schema(description = "Request payload to create a new chat room")
public record CreateRoomRequest(
        @Schema(description = "Unique name of the chat room", example = "general")
        @NotBlank(message = "roomName must not be blank")
        @Size(min = 1, max = 50, message = "roomName must be between 1 and 50 characters")
        @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "roomName must contain only letters, digits, '-' and '_'")
        String roomName
) {}
