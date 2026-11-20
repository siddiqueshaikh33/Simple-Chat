package com.example.chatapp.exception;

/**
 * Thrown when a request provides a limit parameter outside the valid range [1, 100].
 */
public class InvalidLimitException extends RuntimeException {

    public InvalidLimitException(String message) {
        super(message);
    }
}
