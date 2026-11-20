package com.example.chatapp.repository;

import com.example.chatapp.config.RedisKeys;
import com.example.chatapp.exception.RoomAlreadyExistsException;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Repository;

/**
 * Redis-backed implementation of {@link ChatRoomRepository}.
 * <p>
 * Directly executes Redis data structure commands (Set and Hash) via {@link StringRedisTemplate}.
 * Adheres strictly to Section 5 of AGENTS.md for key design and duplicate prevention.
 */
@Repository
public class RedisChatRoomRepository implements ChatRoomRepository {

    private static final Logger log = LoggerFactory.getLogger(RedisChatRoomRepository.class);

    private static final String FIELD_ROOM_ID = "roomId";
    private static final String FIELD_ROOM_NAME = "roomName";
    private static final String FIELD_CREATED_AT = "createdAt";

    private static final String CREATE_ROOM_LUA =
            "local added = redis.call('SADD', KEYS[1], ARGV[1])\n" +
            "if added == 0 then\n" +
            "    return 0\n" +
            "end\n" +
            "redis.call('HSET', KEYS[2], 'roomId', ARGV[1], 'roomName', ARGV[2], 'createdAt', ARGV[3])\n" +
            "return 1\n";

    private static final RedisScript<Long> CREATE_ROOM_SCRIPT =
            new DefaultRedisScript<>(CREATE_ROOM_LUA, Long.class);

    private final StringRedisTemplate redisTemplate;

    /**
     * Constructor injection for StringRedisTemplate.
     */
    public RedisChatRoomRepository(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Atomically adds the roomId to the master room names Set and writes room metadata to Hash.
     * Uses an atomic Redis Lua script so that if duplicate, returns 0 and throws {@link RoomAlreadyExistsException}.
     * If the server crashes or disconnects during creation, no orphan entries are left behind.
     */
    @Override
    public void saveRoom(String roomId, String roomName, Instant createdAt) {
        String metaKey = RedisKeys.roomMetadataKey(roomId);
        List<String> keys = List.of(RedisKeys.ROOM_NAMES_SET, metaKey);

        Long result = redisTemplate.execute(
                CREATE_ROOM_SCRIPT,
                keys,
                roomId,
                roomName,
                createdAt.toString()
        );

        if (result == null || result == 0L) {
            log.warn("Attempted to create existing chat room: {}", roomId);
            throw new RoomAlreadyExistsException(roomId);
        }

        log.info("Successfully registered chat room '{}' with metadata in Redis (atomic Lua)", roomId);
    }

    /**
     * Checks membership in the master chatrooms:names Set in O(1) time.
     */
    @Override
    public boolean existsById(String roomId) {
        return Boolean.TRUE.equals(
                redisTemplate.opsForSet().isMember(RedisKeys.ROOM_NAMES_SET, roomId)
        );
    }

    /**
     * Reads all metadata fields from the room's Hash (HGETALL).
     */
    @Override
    public Optional<Map<String, String>> findMetadataById(String roomId) {
        if (!existsById(roomId)) {
            return Optional.empty();
        }

        Map<Object, Object> rawEntries = redisTemplate.opsForHash().entries(RedisKeys.roomMetadataKey(roomId));
        if (rawEntries.isEmpty()) {
            return Optional.empty();
        }

        Map<String, String> metadata = new HashMap<>();
        rawEntries.forEach((k, v) -> metadata.put(String.valueOf(k), String.valueOf(v)));
        return Optional.of(metadata);
    }

    /**
     * Adds participant to the room's participant Set: SADD chatroom:{roomId}:participants
     */
    @Override
    public boolean addParticipant(String roomId, String participant) {
        String key = RedisKeys.participantsKey(roomId);
        Long added = redisTemplate.opsForSet().add(key, participant);
        return added != null && added > 0L;
    }

    /**
     * Verifies if participant exists in the room's Set: SISMEMBER chatroom:{roomId}:participants
     */
    @Override
    public boolean isParticipant(String roomId, String participant) {
        String key = RedisKeys.participantsKey(roomId);
        return Boolean.TRUE.equals(redisTemplate.opsForSet().isMember(key, participant));
    }

    /**
     * Fetches all participants: SMEMBERS chatroom:{roomId}:participants
     */
    @Override
    public Set<String> getParticipants(String roomId) {
        String key = RedisKeys.participantsKey(roomId);
        Set<String> members = redisTemplate.opsForSet().members(key);
        return members != null ? members : Collections.emptySet();
    }

    /**
     * Deletes all room-related keys from Redis and removes room from master registry.
     */
    @Override
    public boolean deleteRoom(String roomId) {
        if (!existsById(roomId)) {
            return false;
        }

        // Remove from master registry Set
        redisTemplate.opsForSet().remove(RedisKeys.ROOM_NAMES_SET, roomId);

        // Delete metadata Hash, participants Set, and messages List
        List<String> keysToDelete = List.of(
                RedisKeys.roomMetadataKey(roomId),
                RedisKeys.participantsKey(roomId),
                RedisKeys.messagesKey(roomId)
        );
        redisTemplate.delete(keysToDelete);
        log.info("Deleted all Redis keys for chat room '{}'", roomId);
        return true;
    }
}
