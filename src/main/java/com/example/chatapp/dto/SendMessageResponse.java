package com.example.chatapp.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Response payload returned upon successfully sending a message.
 * Matches exact JSON format in Assignment specification:
 * { "message": "Message sent successfully.", "status": "success" }
 *
 * @param message confirmation message
 * @param status  operation status (always "success")
 */
@Schema(description = "Response returned when a message is sent successfully")
public record SendMessageResponse(
        @Schema(description = "Success confirmation message", example = "Message sent successfully.")
        String message,

        @Schema(description = "Response status", example = "success")
        String status
) {
    private static final SendMessageResponse SUCCESS_INSTANCE =
            new SendMessageResponse("Message sent successfully.", "success");

    public static SendMessageResponse ok() {
        return SUCCESS_INSTANCE;
    }
}
