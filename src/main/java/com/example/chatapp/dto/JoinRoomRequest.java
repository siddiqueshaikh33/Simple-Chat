package com.example.chatapp.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request payload for a user to join an existing chat room.
 *
 * @param participant username of the participant (not blank, max 50 characters)
 */
@Schema(description = "Request payload to join an existing chat room")
public record JoinRoomRequest(
        @Schema(description = "Username of the participant joining the room", example = "guest_user")
        @NotBlank(message = "participant must not be blank")
        @Size(max = 50, message = "participant must not exceed 50 characters")
        String participant
) {}
