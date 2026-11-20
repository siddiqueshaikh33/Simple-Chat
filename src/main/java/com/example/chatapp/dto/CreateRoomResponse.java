package com.example.chatapp.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Response payload returned upon successful chat room creation.
 *
 * @param message confirmation message
 * @param roomId  unique lowercase room identifier
 * @param status  operation status (always "success")
 */
@Schema(description = "Response returned when a chat room is created successfully")
public record CreateRoomResponse(
        @Schema(description = "Success confirmation message", example = "Chat room 'general' created successfully.")
        String message,

        @Schema(description = "Created unique lowercase room ID", example = "general")
        String roomId,

        @Schema(description = "Response status", example = "success")
        String status
) {
    /**
     * Convenience factory method to construct a standard success response.
     */
    public static CreateRoomResponse of(String roomId) {
        return new CreateRoomResponse(
                "Chat room '" + roomId + "' created successfully.",
                roomId,
                "success"
        );
    }
}
