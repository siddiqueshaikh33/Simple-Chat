package com.example.chatapp.dto;

import com.example.chatapp.model.ChatMessage;
import java.util.List;

/**
 * Response DTO wrapping the list of chat messages for a room.
 * Adheres to Section 6 of AGENTS.md: {"messages": [...]}.
 */
public record ChatHistoryResponse(List<ChatMessage> messages) {

    public ChatHistoryResponse {
        if (messages == null) {
            messages = List.of();
        }
    }
}
