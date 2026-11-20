package com.example.chatapp.service;

import com.example.chatapp.dto.SendMessageRequest;
import com.example.chatapp.dto.SendMessageResponse;
import com.example.chatapp.exception.NotAParticipantException;
import com.example.chatapp.exception.RoomNotFoundException;
import com.example.chatapp.model.ChatMessage;
import com.example.chatapp.pubsub.MessagePublisher;
import com.example.chatapp.repository.ChatRoomRepository;
import com.example.chatapp.repository.MessageRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Service orchestrating message sending, validation, and history retrieval.
 * Follows Dependency Inversion Principle (DIP) by depending on repository interfaces and Clock.
 */
@Service
public class ChatMessageService {

    private static final Logger log = LoggerFactory.getLogger(ChatMessageService.class);

    private final ChatRoomRepository chatRoomRepository;
    private final MessageRepository messageRepository;
    private final MessagePublisher messagePublisher;
    private final Clock clock;

    public ChatMessageService(
            ChatRoomRepository chatRoomRepository,
            MessageRepository messageRepository,
            MessagePublisher messagePublisher,
            Clock clock) {
        this.chatRoomRepository = chatRoomRepository;
        this.messageRepository = messageRepository;
        this.messagePublisher = messagePublisher;
        this.clock = clock;
    }

    /**
     * Sends a chat message to a room.
     * Appends to the Redis List (history) and broadcasts to Redis Pub/Sub (real-time).
     *
     * @param roomId  unique room identifier
     * @param request payload containing participant and message content
     * @return success response
     * @throws RoomNotFoundException if the target room does not exist
     */
    public SendMessageResponse sendMessage(String roomId, SendMessageRequest request) {
        String normalizedRoomId = normalizeRoomId(roomId);

        // 1. Verify the chat room exists in Redis registry
        if (!chatRoomRepository.existsById(normalizedRoomId)) {
            log.warn("Attempted to send message to non-existent room: {}", normalizedRoomId);
            throw new RoomNotFoundException(normalizedRoomId);
        }

        // 2. Verify sender is a registered participant of the room
        String participant = request.participant().trim();
        if (!chatRoomRepository.isParticipant(normalizedRoomId, participant)) {
            log.warn("Sender '{}' is not a joined participant of room '{}'", participant, normalizedRoomId);
            throw new NotAParticipantException(participant, normalizedRoomId);
        }

        // 3. Generate ISO-8601 UTC timestamp truncated to seconds
        String timestamp = Instant.now(clock).truncatedTo(ChronoUnit.SECONDS).toString();
        String messageText = request.message().trim();

        ChatMessage chatMessage = new ChatMessage(participant, messageText, timestamp);

        // 4. Append to Redis List (authoritative persistent storage)
        messageRepository.saveMessage(normalizedRoomId, chatMessage);

        // 5. Broadcast to Redis Pub/Sub (best-effort real-time delivery)
        try {
            messagePublisher.publish(normalizedRoomId, chatMessage);
        } catch (Exception e) {
            log.error("Failed to broadcast real-time message for room '{}', but message was safely persisted to history",
                    normalizedRoomId, e);
        }

        log.info("Message sent by '{}' to room '{}' at '{}'", participant, normalizedRoomId, timestamp);
        return SendMessageResponse.ok();
    }

    /**
     * Retrieves the last N messages from a room's history in Redis.
     *
     * @param roomId unique room identifier
     * @param limit  maximum number of messages to retrieve
     * @return chronological list of messages
     * @throws RoomNotFoundException if the room does not exist
     */
    public List<ChatMessage> getRecentMessages(String roomId, int limit) {
        String normalizedRoomId = normalizeRoomId(roomId);
        if (!chatRoomRepository.existsById(normalizedRoomId)) {
            log.warn("Attempted to get messages for non-existent room: {}", normalizedRoomId);
            throw new RoomNotFoundException(normalizedRoomId);
        }
        return messageRepository.findRecentMessages(normalizedRoomId, limit);
    }

    private String normalizeRoomId(String roomId) {
        if (roomId == null) {
            throw new IllegalArgumentException("roomId must not be null");
        }
        return roomId.trim().toLowerCase();
    }
}
