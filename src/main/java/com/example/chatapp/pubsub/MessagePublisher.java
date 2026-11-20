package com.example.chatapp.pubsub;

import com.example.chatapp.config.RedisKeys;
import com.example.chatapp.model.ChatMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Publishes chat messages to room-specific Redis Pub/Sub channels for real-time delivery.
 * <p>
 * Follows the Observer Pattern and the Single Responsibility Principle:
 * its sole concern is broadcasting serialized event payloads over Redis channels.
 */
@Component
public class MessagePublisher {

    private static final Logger log = LoggerFactory.getLogger(MessagePublisher.class);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Constructor injection for Redis template and JSON serializer.
     */
    public MessagePublisher(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Serializes a chat message to a clean JSON string and publishes it
     * to the room's dedicated Redis Pub/Sub topic: chatroom:{roomId}:channel.
     *
     * @param roomId  unique room identifier
     * @param message the chat message domain model
     */
    public void publish(String roomId, ChatMessage message) {
        try {
            // 1. Serialize message to clean JSON: {"participant":"...","message":"...","timestamp":"..."}
            String jsonPayload = objectMapper.writeValueAsString(message);

            // 2. Resolve the target channel name from RedisKeys: chatroom:{roomId}:channel
            String channel = RedisKeys.channelTopic(roomId);

            // 3. Redis PUBLISH command: broadcasts payload to all active channel subscribers
            redisTemplate.convertAndSend(channel, jsonPayload);

            log.info("Published message to Redis channel '{}'", channel);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize chat message for room '{}' to JSON", roomId, e);
        }
    }
}
