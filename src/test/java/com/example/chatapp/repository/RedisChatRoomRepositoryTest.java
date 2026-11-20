package com.example.chatapp.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.chatapp.config.RedisKeys;
import com.example.chatapp.exception.RoomAlreadyExistsException;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

@ExtendWith(MockitoExtension.class)
class RedisChatRoomRepositoryTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private SetOperations<String, String> setOperations;

    @Mock
    private HashOperations<String, Object, Object> hashOperations;

    private RedisChatRoomRepository repository;

    @BeforeEach
    void setUp() {
        repository = new RedisChatRoomRepository(redisTemplate);
    }

    @Test
    @DisplayName("saveRoom: successfully saves room atomically via Lua script")
    void testSaveRoom_Success() {
        when(redisTemplate.execute(any(), anyList(), eq("room-1"), eq("General"), any()))
                .thenReturn(1L);

        repository.saveRoom("room-1", "General", Instant.now());

        verify(redisTemplate).execute(
                any(),
                eq(List.of(RedisKeys.ROOM_NAMES_SET, RedisKeys.roomMetadataKey("room-1"))),
                eq("room-1"),
                eq("General"),
                any()
        );
    }

    @Test
    @DisplayName("saveRoom: throws RoomAlreadyExistsException when Lua returns 0")
    void testSaveRoom_Duplicate_ThrowsException() {
        when(redisTemplate.execute(any(), anyList(), eq("room-1"), eq("General"), any()))
                .thenReturn(0L);

        assertThatThrownBy(() -> repository.saveRoom("room-1", "General", Instant.now()))
                .isInstanceOf(RoomAlreadyExistsException.class)
                .hasMessageContaining("room-1");
    }

    @Test
    @DisplayName("existsById: returns true when room is in Set")
    void testExistsById() {
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(setOperations.isMember(RedisKeys.ROOM_NAMES_SET, "room-1")).thenReturn(true);

        assertThat(repository.existsById("room-1")).isTrue();
    }

    @Test
    @DisplayName("findMetadataById: returns metadata when room exists")
    void testFindMetadataById_Exists() {
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(setOperations.isMember(RedisKeys.ROOM_NAMES_SET, "room-1")).thenReturn(true);
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(hashOperations.entries(RedisKeys.roomMetadataKey("room-1")))
                .thenReturn(Map.of("roomId", "room-1", "roomName", "General"));

        Optional<Map<String, String>> metadata = repository.findMetadataById("room-1");
        assertThat(metadata).isPresent();
        assertThat(metadata.get()).containsEntry("roomId", "room-1");
        assertThat(metadata.get()).containsEntry("roomName", "General");
    }

    @Test
    @DisplayName("findMetadataById: returns empty when room does not exist")
    void testFindMetadataById_NotExists() {
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(setOperations.isMember(RedisKeys.ROOM_NAMES_SET, "non-existent")).thenReturn(false);

        Optional<Map<String, String>> metadata = repository.findMetadataById("non-existent");
        assertThat(metadata).isEmpty();
    }

    @Test
    @DisplayName("addParticipant: adds participant to room set")
    void testAddParticipant() {
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(setOperations.add(RedisKeys.participantsKey("room-1"), "Alice")).thenReturn(1L);

        boolean added = repository.addParticipant("room-1", "Alice");
        assertThat(added).isTrue();
    }

    @Test
    @DisplayName("isParticipant: returns true when participant is present")
    void testIsParticipant() {
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(setOperations.isMember(RedisKeys.participantsKey("room-1"), "Alice")).thenReturn(true);

        assertThat(repository.isParticipant("room-1", "Alice")).isTrue();
    }

    @Test
    @DisplayName("getParticipants: returns participants set")
    void testGetParticipants() {
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(setOperations.members(RedisKeys.participantsKey("room-1"))).thenReturn(Set.of("Alice", "Bob"));

        Set<String> participants = repository.getParticipants("room-1");
        assertThat(participants).containsExactlyInAnyOrder("Alice", "Bob");
    }

    @Test
    @DisplayName("deleteRoom: removes room and deletes keys atomically when room exists")
    void testDeleteRoom_Exists() {
        when(redisTemplate.execute(any(), anyList(), eq("room-1")))
                .thenReturn(1L);

        boolean deleted = repository.deleteRoom("room-1");
        assertThat(deleted).isTrue();

        verify(redisTemplate).execute(
                any(),
                eq(List.of(
                        RedisKeys.ROOM_NAMES_SET,
                        RedisKeys.roomMetadataKey("room-1"),
                        RedisKeys.participantsKey("room-1"),
                        RedisKeys.messagesKey("room-1")
                )),
                eq("room-1")
        );
    }

    @Test
    @DisplayName("deleteRoom: returns false when room does not exist")
    void testDeleteRoom_NotExists() {
        when(redisTemplate.execute(any(), anyList(), eq("non-existent")))
                .thenReturn(0L);

        boolean deleted = repository.deleteRoom("non-existent");
        assertThat(deleted).isFalse();
    }
}
