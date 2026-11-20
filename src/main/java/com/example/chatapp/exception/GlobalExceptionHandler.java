package com.example.chatapp.exception;

import com.example.chatapp.dto.ErrorResponse;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Centralized exception handler mapping domain and validation errors to standardized JSON responses.
 * Adheres to Section 7 of AGENTS.md.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handles duplicate room creation attempts, returning HTTP 409 Conflict.
     */
    @ExceptionHandler(RoomAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleRoomAlreadyExists(RoomAlreadyExistsException ex) {
        log.warn("Duplicate room conflict: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of(ex.getMessage()));
    }

    /**
     * Handles operations on non-existent chat rooms, returning HTTP 404 Not Found.
     */
    @ExceptionHandler(RoomNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleRoomNotFound(RoomNotFoundException ex) {
        log.warn("Room not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of(ex.getMessage()));
    }

    /**
     * Handles unauthorized message attempts by users who haven't joined the room, returning HTTP 403 Forbidden.
     */
    @ExceptionHandler(NotAParticipantException.class)
    public ResponseEntity<ErrorResponse> handleNotAParticipant(NotAParticipantException ex) {
        log.warn("Unauthorized sender attempt: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ErrorResponse.of(ex.getMessage()));
    }

    /**
     * Handles invalid limit parameter in message history retrieval, returning HTTP 400 Bad Request.
     */
    @ExceptionHandler(InvalidLimitException.class)
    public ResponseEntity<ErrorResponse> handleInvalidLimit(InvalidLimitException ex) {
        log.warn("Invalid limit parameter: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(ex.getMessage()));
    }

    /**
     * Handles input validation failures (@Valid annotations), returning HTTP 400 Bad Request.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(MethodArgumentNotValidException ex) {
        List<String> details = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + (err.getDefaultMessage() != null ? err.getDefaultMessage() : "invalid value"))
                .toList();
        log.warn("Input validation failure: {}", details);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of("Validation failed.", details));
    }

    /**
     * Handles malformed or missing HTTP request bodies, returning HTTP 400 Bad Request.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleMessageNotReadable(HttpMessageNotReadableException ex) {
        log.warn("Malformed or missing HTTP request body: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of("Required request body is missing or malformed."));
    }

    /**
     * Handles parameter type conversion failures, returning HTTP 400 Bad Request.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        log.warn("Method argument type mismatch for parameter '{}': {}", ex.getName(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of("Invalid parameter: " + ex.getName()));
    }

    /**
     * Handles unmapped resource/route requests, returning HTTP 404 Not Found.
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResourceFound(NoResourceFoundException ex) {
        log.warn("Resource path not found: {}", ex.getResourcePath());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of("Resource not found: " + ex.getResourcePath()));
    }

    /**
     * Handles unsupported HTTP methods (e.g. GET instead of POST), returning HTTP 405 Method Not Allowed.
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        log.warn("Method not allowed: {} on supported methods: {}", ex.getMethod(), ex.getSupportedHttpMethods());
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(ErrorResponse.of("HTTP method '" + ex.getMethod() + "' not supported for this endpoint."));
    }

    /**
     * Handles unsupported media types (e.g. non-JSON payloads), returning HTTP 415 Unsupported Media Type.
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex) {
        log.warn("Unsupported media type: {}", ex.getContentType());
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .body(ErrorResponse.of("Content type '" + ex.getContentType() + "' not supported. Expected 'application/json'."));
    }

    /**
     * Handles missing required request parameters, returning HTTP 400 Bad Request.
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParameter(MissingServletRequestParameterException ex) {
        log.warn("Missing request parameter: {}", ex.getParameterName());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of("Missing required request parameter: " + ex.getParameterName()));
    }

    /**
     * Handles Redis connection or infrastructure outages, returning HTTP 503 Service Unavailable.
     */
    @ExceptionHandler({RedisConnectionFailureException.class, DataAccessException.class})
    public ResponseEntity<ErrorResponse> handleRedisUnavailable(Exception ex) {
        log.error("Redis datastore service unavailable: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ErrorResponse.of("Database service is temporarily unavailable. Please try again later."));
    }

    /**
     * Fallback for any unexpected internal exceptions, returning HTTP 500 without leaking stack traces.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
        log.error("Unhandled internal server error", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.of("Unexpected error."));
    }
}
