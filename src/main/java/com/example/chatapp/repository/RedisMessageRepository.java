package com.example.chatapp.repository;

import com.example.chatapp.config.RedisKeys;
import com.example.chatapp.model.ChatMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

/**
 * Redis-backed implementation of {@link MessageRepository} using Redis List data structure.
 * Messages are stored as clean JSON strings in chronological order.
 */
@Repository
public class RedisMessageRepository implements MessageRepository {

    private static final Logger log = LoggerFactory.getLogger(RedisMessageRepository.class);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public RedisMessageRepository(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void saveMessage(String roomId, ChatMessage message) {
        try {
            // Serialize to clean JSON string: {"participant":"...","message":"...","timestamp":"..."}
            String json = objectMapper.writeValueAsString(message);
            String listKey = RedisKeys.messagesKey(roomId);

            // RPUSH: Appends message to the end of the Redis list in O(1) time
            redisTemplate.opsForList().rightPush(listKey, json);

            // LTRIM: Keeps only the latest 1,000 messages (Section 5 of AGENTS.md)
            redisTemplate.opsForList().trim(listKey, -RedisKeys.MAX_MESSAGES_PER_ROOM, -1);
            log.info("Appended message for participant '{}' to room '{}'", message.participant(), roomId);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize chat message to JSON", e);
            throw new IllegalStateException("Failed to serialize message payload", e);
        }
    }

    @Override
    public List<ChatMessage> findRecentMessages(String roomId, int limit) {
        String listKey = RedisKeys.messagesKey(roomId);

        // LRANGE key -limit -1: Fetches the last 'limit' items oldest to newest in O(k) time
        List<String> rawMessages = redisTemplate.opsForList().range(listKey, -limit, -1);
        if (rawMessages == null || rawMessages.isEmpty()) {
            return Collections.emptyList();
        }

        List<ChatMessage> result = new ArrayList<>(rawMessages.size());
        for (String rawJson : rawMessages) {
            try {
                result.add(objectMapper.readValue(rawJson, ChatMessage.class));
            } catch (JsonProcessingException e) {
                log.warn("Corrupt JSON message in room '{}', skipping entry", roomId, e);
            }
        }
        return result;
    }
}
