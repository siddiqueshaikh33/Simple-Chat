package com.example.chatapp.config;

/**
 * Centralized registry of all Redis key patterns and limits used across the application.
 * Follows the Single Responsibility Principle by serving as the single source of truth for key formatting.
 */
public final class RedisKeys {

    private RedisKeys() {
        // Utility class: prevent instantiation
    }

    /**
     * Redis Set storing all unique, registered room names/IDs.
     * Used for O(1) duplicate detection and existence verification.
     */
    public static final String ROOM_NAMES_SET = "chatrooms:names";

    /**
     * Pub/Sub channel pattern matching all room channels for real-time broadcasting.
     */
    public static final String CHATROOM_CHANNEL_PATTERN = "chatroom:*:channel";

    /**
     * Maximum number of messages preserved in a room's history list to prevent unbounded memory growth.
     */
    public static final int MAX_MESSAGES_PER_ROOM = 1000;

    /**
     * Key prefix constants for building room-specific keys.
     */
    private static final String CHATROOM_PREFIX = "chatroom:";
    private static final String PARTICIPANTS_SUFFIX = ":participants";
    private static final String MESSAGES_SUFFIX = ":messages";
    private static final String CHANNEL_SUFFIX = ":channel";

    /**
     * Generates the Redis Hash key for a room's metadata (fields: roomId, roomName, createdAt).
     *
     * @param roomId unique lowercase room identifier
     * @return Redis key string "chatroom:{roomId}"
     */
    public static String roomMetadataKey(String roomId) {
        return CHATROOM_PREFIX + normalizeRoomId(roomId);
    }

    /**
     * Generates the Redis Set key storing all participants who joined a room.
     *
     * @param roomId unique lowercase room identifier
     * @return Redis key string "chatroom:{roomId}:participants"
     */
    public static String participantsKey(String roomId) {
        return CHATROOM_PREFIX + normalizeRoomId(roomId) + PARTICIPANTS_SUFFIX;
    }

    /**
     * Generates the Redis List key storing chat messages in chronological order.
     *
     * @param roomId unique lowercase room identifier
     * @return Redis key string "chatroom:{roomId}:messages"
     */
    public static String messagesKey(String roomId) {
        return CHATROOM_PREFIX + normalizeRoomId(roomId) + MESSAGES_SUFFIX;
    }

    /**
     * Generates the Redis Pub/Sub channel name for broadcasting real-time room messages.
     *
     * @param roomId unique lowercase room identifier
     * @return Redis channel string "chatroom:{roomId}:channel"
     */
    public static String channelTopic(String roomId) {
        return CHATROOM_PREFIX + normalizeRoomId(roomId) + CHANNEL_SUFFIX;
    }

    /**
     * Extracts the normalized roomId from a Redis Pub/Sub channel string.
     * e.g. "chatroom:tech:channel" -> "tech"
     *
     * @param channel the channel string
     * @return roomId if valid pattern, or null otherwise
     */
    public static String extractRoomIdFromChannel(String channel) {
        if (channel != null && channel.startsWith(CHATROOM_PREFIX) && channel.endsWith(CHANNEL_SUFFIX)) {
            return channel.substring(CHATROOM_PREFIX.length(), channel.length() - CHANNEL_SUFFIX.length());
        }
        return null;
    }

    /**
     * Helper to ensure roomId is consistently trimmed and lowercased.
     */
    private static String normalizeRoomId(String roomId) {
        if (roomId == null) {
            throw new IllegalArgumentException("roomId must not be null");
        }
        return roomId.trim().toLowerCase();
    }
}
