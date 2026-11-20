package com.example.chatapp.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * Standard unified error response payload used across all endpoints.
 * Conforms to Section 7 of AGENTS.md.
 *
 * @param status  always "error"
 * @param message description of the error
 * @param details optional list of field-level validation errors (omitted when empty)
 */
@Schema(description = "Standardized error response payload")
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ErrorResponse(
        @Schema(description = "Response status", example = "error")
        String status,

        @Schema(description = "High-level error explanation", example = "Chat room 'general' already exists.")
        String message,

        @Schema(description = "Detailed list of field validation errors, if applicable")
        List<String> details
) {
    /**
     * Factory method for standard single-message errors (e.g. 404, 409, 500).
     */
    public static ErrorResponse of(String message) {
        return new ErrorResponse("error", message, null);
    }

    /**
     * Factory method for validation errors with field details (e.g. 400).
     */
    public static ErrorResponse of(String message, List<String> details) {
        return new ErrorResponse("error", message, details);
    }
}
