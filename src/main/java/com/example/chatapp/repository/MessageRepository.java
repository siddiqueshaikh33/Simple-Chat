package com.example.chatapp.repository;

import com.example.chatapp.model.ChatMessage;
import java.util.List;

/**
 * Repository interface for persisting and reading chat messages from Redis.
 * Follows the Dependency Inversion Principle (DIP).
 */
public interface MessageRepository {

    /**
     * Appends a chat message to the room's message list in Redis (RPUSH)
     * and trims older entries beyond MAX_MESSAGES_PER_ROOM (LTRIM).
     *
     * @param roomId  unique room identifier
     * @param message domain chat message
     */
    void saveMessage(String roomId, ChatMessage message);

    /**
     * Retrieves the last N messages from the room's Redis list (LRANGE -limit -1).
     *
     * @param roomId unique room identifier
     * @param limit  maximum number of messages to retrieve (1 to 100)
     * @return chronological list of messages from oldest to newest within the window
     */
    List<ChatMessage> findRecentMessages(String roomId, int limit);
}
