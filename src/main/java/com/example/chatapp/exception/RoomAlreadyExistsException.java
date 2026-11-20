package com.example.chatapp.exception;

/**
 * Thrown when an attempt is made to create a chat room with a name/ID that already exists.
 * Mapped to HTTP 409 Conflict.
 */
public class RoomAlreadyExistsException extends RuntimeException {

    private final String roomId;

    public RoomAlreadyExistsException(String roomId) {
        super("Chat room '" + roomId + "' already exists.");
        this.roomId = roomId;
    }

    public String getRoomId() {
        return roomId;
    }
}
