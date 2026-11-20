package com.example.chatapp.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Response payload returned upon successfully joining a chat room.
 * Matches exact JSON format in Assignment specification:
 * { "message": "User '{participant}' joined chat room '{roomId}'.", "status": "success" }
 *
 * @param message confirmation message
 * @param status  operation status (always "success")
 */
@Schema(description = "Response returned when a user successfully joins a chat room")
public record JoinRoomResponse(
        @Schema(description = "Success confirmation message", example = "User 'guest_user' joined chat room 'general'.")
        String message,

        @Schema(description = "Response status", example = "success")
        String status
) {
    /**
     * Factory method to construct the exact response string specified in assignment.
     */
    public static JoinRoomResponse of(String participant, String roomId) {
        return new JoinRoomResponse(
                "User '" + participant + "' joined chat room '" + roomId + "'.",
                "success"
        );
    }
}
