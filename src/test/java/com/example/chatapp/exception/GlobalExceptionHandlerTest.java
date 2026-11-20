package com.example.chatapp.exception;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.chatapp.dto.ErrorResponse;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
    }

    @Test
    @DisplayName("handleRoomAlreadyExists maps to 409 Conflict with standard error shape")
    void handleRoomAlreadyExists_returns409Conflict() {
        RoomAlreadyExistsException ex = new RoomAlreadyExistsException("general");
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleRoomAlreadyExists(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo("error");
        assertThat(response.getBody().message()).isEqualTo("Chat room 'general' already exists.");
        assertThat(response.getBody().details()).isNull();
    }

    @Test
    @DisplayName("handleRoomNotFound maps to 404 Not Found with standard error shape")
    void handleRoomNotFound_returns404NotFound() {
        RoomNotFoundException ex = new RoomNotFoundException("missing");
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleRoomNotFound(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo("error");
        assertThat(response.getBody().message()).isEqualTo("Chat room 'missing' not found.");
        assertThat(response.getBody().details()).isNull();
    }

    @Test
    @DisplayName("handleNotAParticipant maps to 403 Forbidden with standard error shape")
    void handleNotAParticipant_returns403Forbidden() {
        NotAParticipantException ex = new NotAParticipantException("stranger", "general");
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleNotAParticipant(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo("error");
        assertThat(response.getBody().message()).isEqualTo("User 'stranger' is not a participant of chat room 'general'.");
        assertThat(response.getBody().details()).isNull();
    }

    @Test
    @DisplayName("handleInvalidLimit maps to 400 Bad Request")
    void handleInvalidLimit_returns400BadRequest() {
        InvalidLimitException ex = new InvalidLimitException("Limit must be between 1 and 100.");
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleInvalidLimit(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo("error");
        assertThat(response.getBody().message()).isEqualTo("Limit must be between 1 and 100.");
    }

    @Test
    @DisplayName("handleValidationErrors maps to 400 Bad Request with field details")
    void handleValidationErrors_returns400BadRequestWithDetails() throws NoSuchMethodException {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "target");
        bindingResult.addError(new FieldError("target", "roomName", "must not be blank"));

        MethodParameter parameter = new MethodParameter(
                GlobalExceptionHandlerTest.class.getDeclaredMethod("setUp"), -1);
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(parameter, bindingResult);

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleValidationErrors(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo("error");
        assertThat(response.getBody().message()).isEqualTo("Validation failed.");
        assertThat(response.getBody().details()).containsExactly("roomName: must not be blank");
    }

    @Test
    @DisplayName("handleMessageNotReadable maps to 400 Bad Request")
    void handleMessageNotReadable_returns400BadRequest() {
        HttpMessageNotReadableException ex = new HttpMessageNotReadableException("Malformed JSON");
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleMessageNotReadable(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo("error");
        assertThat(response.getBody().message()).isEqualTo("Required request body is missing or malformed.");
    }

    @Test
    @DisplayName("handleTypeMismatch maps to 400 Bad Request with parameter name")
    void handleTypeMismatch_returns400BadRequest() {
        MethodArgumentTypeMismatchException ex = new MethodArgumentTypeMismatchException(
                "abc", Integer.class, "limit", null, null);
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleTypeMismatch(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo("error");
        assertThat(response.getBody().message()).isEqualTo("Invalid parameter: limit");
    }

    @Test
    @DisplayName("handleNoResourceFound maps to 404 Not Found")
    void handleNoResourceFound_returns404NotFound() {
        NoResourceFoundException ex = new NoResourceFoundException(HttpMethod.GET, "/unknown/path");
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleNoResourceFound(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo("error");
        assertThat(response.getBody().message()).isEqualTo("Resource not found: /unknown/path");
    }

    @Test
    @DisplayName("handleMethodNotSupported maps to 405 Method Not Allowed")
    void handleMethodNotSupported_returns405MethodNotAllowed() {
        HttpRequestMethodNotSupportedException ex = new HttpRequestMethodNotSupportedException("GET", List.of("POST"));
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleMethodNotSupported(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo("error");
        assertThat(response.getBody().message()).contains("HTTP method 'GET' not supported");
    }

    @Test
    @DisplayName("handleMediaTypeNotSupported maps to 415 Unsupported Media Type")
    void handleMediaTypeNotSupported_returns415UnsupportedMediaType() {
        HttpMediaTypeNotSupportedException ex = new HttpMediaTypeNotSupportedException(
                MediaType.TEXT_PLAIN,
                List.of(MediaType.APPLICATION_JSON));
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleMediaTypeNotSupported(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo("error");
        assertThat(response.getBody().message()).contains("Content type 'text/plain' not supported");
    }

    @Test
    @DisplayName("handleMissingParameter maps to 400 Bad Request")
    void handleMissingParameter_returns400BadRequest() {
        MissingServletRequestParameterException ex = new MissingServletRequestParameterException("roomId", "String");
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleMissingParameter(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo("error");
        assertThat(response.getBody().message()).isEqualTo("Missing required request parameter: roomId");
    }

    @Test
    @DisplayName("handleUnexpected maps to 500 Internal Server Error without leaking internal message")
    void handleUnexpected_returns500InternalServerError() {
        RuntimeException ex = new RuntimeException("Sensitive database connection failure");
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleUnexpected(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo("error");
        assertThat(response.getBody().message()).isEqualTo("Unexpected error.");
    }
}
