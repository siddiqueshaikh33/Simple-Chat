package com.example.chatapp.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Standard response payload returned after a chat room is deleted.
 * Conforms to Section 6 of AGENTS.md and Test Case 5 of the specification.
 */
@Schema(description = "Response payload after deleting a chat room")
public record DeleteRoomResponse(
        @Schema(description = "Confirmation message", example = "Chat room 'general' deleted successfully.")
        String message,

        @Schema(description = "Operation status", example = "success")
        String status
) {
    public static DeleteRoomResponse of(String roomId) {
        return new DeleteRoomResponse("Chat room '" + roomId + "' deleted successfully.", "success");
    }
}
