package com.example.chatapp.service;

import com.example.chatapp.dto.CreateRoomRequest;
import com.example.chatapp.dto.CreateRoomResponse;
import com.example.chatapp.dto.DeleteRoomResponse;
import com.example.chatapp.dto.JoinRoomRequest;
import com.example.chatapp.dto.JoinRoomResponse;
import com.example.chatapp.exception.RoomNotFoundException;
import com.example.chatapp.repository.ChatRoomRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Service orchestrating chat room business operations.
 * <p>
 * Decoupled from Redis storage via {@link ChatRoomRepository} and depends on
 * {@link Clock} for deterministic timestamp generation (SOLID DIP).
 */
@Service
public class ChatRoomService {

    private static final Logger log = LoggerFactory.getLogger(ChatRoomService.class);

    private final ChatRoomRepository chatRoomRepository;
    private final Clock clock;

    public ChatRoomService(ChatRoomRepository chatRoomRepository, Clock clock) {
        this.chatRoomRepository = chatRoomRepository;
        this.clock = clock;
    }

    /**
     * Creates a new chat room.
     *
     * @param request payload containing the room name
     * @return success response containing the generated roomId
     * @throws com.example.chatapp.exception.RoomAlreadyExistsException if the room already exists
     */
    public CreateRoomResponse createRoom(CreateRoomRequest request) {
        String trimmedName = request.roomName().trim();
        String roomId = trimmedName.toLowerCase();
        Instant createdAt = Instant.now(clock).truncatedTo(ChronoUnit.SECONDS);

        log.info("Creating chat room with name '{}' (id: '{}')", trimmedName, roomId);
        chatRoomRepository.saveRoom(roomId, trimmedName, createdAt);

        return CreateRoomResponse.of(roomId);
    }

    /**
     * Adds a participant to an existing chat room.
     *
     * @param roomId  unique lowercase room identifier
     * @param request payload containing the participant's username
     * @return success response indicating the user joined
     * @throws RoomNotFoundException if the room does not exist
     */
    public JoinRoomResponse joinRoom(String roomId, JoinRoomRequest request) {
        String normalizedRoomId = normalizeRoomId(roomId);
        if (!roomExists(normalizedRoomId)) {
            log.warn("Attempted to join non-existent room: {}", normalizedRoomId);
            throw new RoomNotFoundException(normalizedRoomId);
        }

        String participant = request.participant().trim();
        log.info("User '{}' joining chat room '{}'", participant, normalizedRoomId);
        chatRoomRepository.addParticipant(normalizedRoomId, participant);

        return JoinRoomResponse.of(participant, normalizedRoomId);
    }

    /**
     * Deletes a chat room and all associated Redis data structures.
     *
     * @param roomId unique room identifier
     * @return confirmation response
     * @throws RoomNotFoundException if the target room does not exist
     */
    public DeleteRoomResponse deleteRoom(String roomId) {
        String normalizedRoomId = normalizeRoomId(roomId);
        log.info("Deleting chat room '{}'", normalizedRoomId);

        boolean deleted = chatRoomRepository.deleteRoom(normalizedRoomId);
        if (!deleted) {
            log.warn("Attempted to delete non-existent room: {}", normalizedRoomId);
            throw new RoomNotFoundException(normalizedRoomId);
        }

        return DeleteRoomResponse.of(normalizedRoomId);
    }

    /**
     * Verifies if a chat room exists.
     *
     * @param roomId unique lowercase room identifier
     * @return true if room exists, false otherwise
     */
    public boolean roomExists(String roomId) {
        if (roomId == null) {
            return false;
        }
        return chatRoomRepository.existsById(roomId.trim().toLowerCase());
    }

    private String normalizeRoomId(String roomId) {
        if (roomId == null) {
            throw new IllegalArgumentException("roomId must not be null");
        }
        return roomId.trim().toLowerCase();
    }
}
