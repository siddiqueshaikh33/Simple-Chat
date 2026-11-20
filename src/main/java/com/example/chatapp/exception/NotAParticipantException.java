package com.example.chatapp.exception;

/**
 * Thrown when a user attempts to send a message to a room without having joined it first.
 * Maps to HTTP 403 Forbidden.
 */
public class NotAParticipantException extends RuntimeException {

    public NotAParticipantException(String participant, String roomId) {
        super("User '" + participant + "' is not a participant of chat room '" + roomId + "'.");
    }
}
