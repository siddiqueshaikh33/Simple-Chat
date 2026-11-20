package com.example.chatapp.repository;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Repository interface defining operations for chat room metadata,
 * room existence, and room participant management.
 * <p>
 * Follows the Dependency Inversion Principle (DIP) and Repository Pattern,
 * allowing services to depend on abstractions rather than low-level Redis calls.
 */
public interface ChatRoomRepository {

    /**
     * Atomically registers a chat room and stores its metadata.
     *
     * @param roomId    the unique lowercase identifier of the room (e.g. "general")
     * @param roomName  the display name of the room (e.g. "General")
     * @param createdAt the timestamp when the room was created
     * @throws com.example.chatapp.exception.RoomAlreadyExistsException if the roomId is already registered
     */
    void saveRoom(String roomId, String roomName, Instant createdAt);

    /**
     * Checks if a chat room exists in the system registry.
     *
     * @param roomId the unique lowercase identifier of the room
     * @return true if the room exists, false otherwise
     */
    boolean existsById(String roomId);

    /**
     * Retrieves the metadata hash for a chat room.
     *
     * @param roomId the unique lowercase identifier of the room
     * @return an Optional containing the metadata map (roomId, roomName, createdAt), or empty if not found
     */
    Optional<Map<String, String>> findMetadataById(String roomId);

    /**
     * Adds a participant username to the room's participant Set.
     *
     * @param roomId      the room to join
     * @param participant the username joining the room
     * @return true if the participant was newly added, false if already a member
     */
    boolean addParticipant(String roomId, String participant);

    /**
     * Checks if a user is currently a joined participant in the room.
     *
     * @param roomId      the unique room identifier
     * @param participant the username to verify
     * @return true if the user is a joined participant, false otherwise
     */
    boolean isParticipant(String roomId, String participant);

    /**
     * Retrieves all joined participants for a given chat room.
     *
     * @param roomId the unique room identifier
     * @return a Set of participant usernames
     */
    Set<String> getParticipants(String roomId);

    /**
     * Deletes all keys associated with a chat room (registry, metadata, participants, messages).
     *
     * @param roomId the room to delete
     * @return true if the room existed and was deleted, false otherwise
     */
    boolean deleteRoom(String roomId);
}
