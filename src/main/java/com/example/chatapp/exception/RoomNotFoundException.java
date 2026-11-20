package com.example.chatapp.exception;

/**
 * Thrown when an operation targets a chat room that does not exist.
 * Mapped to HTTP 404 Not Found.
 */
public class RoomNotFoundException extends RuntimeException {

    private final String roomId;

    public RoomNotFoundException(String roomId) {
        super("Chat room '" + roomId + "' not found.");
        this.roomId = roomId;
    }

    public String getRoomId() {
        return roomId;
    }
}
