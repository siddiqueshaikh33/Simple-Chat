package com.example.chatapp.controller;

import com.example.chatapp.dto.ChatHistoryResponse;
import com.example.chatapp.dto.CreateRoomRequest;
import com.example.chatapp.dto.CreateRoomResponse;
import com.example.chatapp.dto.DeleteRoomResponse;
import com.example.chatapp.dto.ErrorResponse;
import com.example.chatapp.dto.JoinRoomRequest;
import com.example.chatapp.dto.JoinRoomResponse;
import com.example.chatapp.dto.SendMessageRequest;
import com.example.chatapp.dto.SendMessageResponse;
import com.example.chatapp.exception.InvalidLimitException;
import com.example.chatapp.model.ChatMessage;
import com.example.chatapp.service.ChatMessageService;
import com.example.chatapp.service.ChatRoomService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller exposing endpoints for chat room management and messaging.
 * Conforms strictly to Section 6 of AGENTS.md.
 */
@RestController
@RequestMapping("/api/chatapp")
@Tag(name = "Chat Rooms", description = "Endpoints for creating, joining, and messaging in chat rooms")
public class ChatRoomController {

    private static final Logger log = LoggerFactory.getLogger(ChatRoomController.class);

    private final ChatRoomService chatRoomService;
    private final ChatMessageService chatMessageService;

    public ChatRoomController(ChatRoomService chatRoomService, ChatMessageService chatMessageService) {
        this.chatRoomService = chatRoomService;
        this.chatMessageService = chatMessageService;
    }

    /**
     * Creates a new chat room.
     *
     * @param request payload containing the roomName
     * @return HTTP 201 Created with room details on success
     */
    @PostMapping("/chatrooms")
    @Operation(summary = "Create a new chat room", description = "Creates a new chat room with a unique name.")
    @ApiResponse(responseCode = "201", description = "Chat room created successfully",
            content = @Content(schema = @Schema(implementation = CreateRoomResponse.class)))
    @ApiResponse(responseCode = "400", description = "Invalid room name",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "Duplicate room name",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<CreateRoomResponse> createRoom(@Valid @RequestBody CreateRoomRequest request) {
        log.info("REST request to create room: {}", request.roomName());
        CreateRoomResponse response = chatRoomService.createRoom(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Allows a participant to join an existing chat room.
     *
     * @param roomId  the target chat room identifier
     * @param request payload containing the participant name
     * @return HTTP 200 OK with success confirmation
     */
    @PostMapping("/chatrooms/{roomId}/join")
    @Operation(summary = "Join a chat room", description = "Adds a participant to the specified existing chat room.")
    @ApiResponse(responseCode = "200", description = "User joined chat room successfully",
            content = @Content(schema = @Schema(implementation = JoinRoomResponse.class)))
    @ApiResponse(responseCode = "400", description = "Invalid or blank participant",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Chat room not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<JoinRoomResponse> joinRoom(
            @PathVariable String roomId,
            @Valid @RequestBody JoinRoomRequest request) {
        log.info("REST request for user '{}' to join room '{}'", request.participant(), roomId);
        JoinRoomResponse response = chatRoomService.joinRoom(roomId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Sends a chat message to the specified room.
     * Appends to Redis List and broadcasts over Redis Pub/Sub.
     *
     * @param roomId  the target chat room identifier
     * @param request payload containing participant and message
     * @return HTTP 200 OK on success
     */
    @PostMapping("/chatrooms/{roomId}/messages")
    @Operation(summary = "Send a message", description = "Sends a message to a chat room and broadcasts it via Redis Pub/Sub.")
    @ApiResponse(responseCode = "200", description = "Message sent successfully",
            content = @Content(schema = @Schema(implementation = SendMessageResponse.class)))
    @ApiResponse(responseCode = "400", description = "Invalid participant or message",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "403", description = "Sender is not a joined participant",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Chat room not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<SendMessageResponse> sendMessage(
            @PathVariable String roomId,
            @Valid @RequestBody SendMessageRequest request) {
        log.info("REST request to send message to room '{}' by user '{}'", roomId, request.participant());
        SendMessageResponse response = chatMessageService.sendMessage(roomId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves the last N messages from the chat room in chronological order.
     *
     * @param roomId the target chat room identifier
     * @param limit  the number of recent messages to return (default: 10, min: 1, max: 100)
     * @return HTTP 200 OK with the list of messages wrapped in ChatHistoryResponse
     */
    @GetMapping("/chatrooms/{roomId}/messages")
    @Operation(summary = "Get message history", description = "Retrieves the last N messages from the chat room in chronological order (oldest to newest).")
    @ApiResponse(responseCode = "200", description = "Messages retrieved successfully",
            content = @Content(schema = @Schema(implementation = ChatHistoryResponse.class)))
    @ApiResponse(responseCode = "400", description = "Invalid limit parameter (must be between 1 and 100)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Chat room not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<ChatHistoryResponse> getMessages(
            @PathVariable String roomId,
            @RequestParam(defaultValue = "10") int limit) {
        if (limit <= 0 || limit > 100) {
            throw new InvalidLimitException("Limit must be between 1 and 100.");
        }
        log.info("REST request to get recent {} messages for room '{}'", limit, roomId);
        List<ChatMessage> messages = chatMessageService.getRecentMessages(roomId, limit);
        return ResponseEntity.ok(new ChatHistoryResponse(messages));
    }

    /**
     * Deletes a chat room and removes all associated Redis keys.
     *
     * @param roomId the target chat room identifier
     * @return HTTP 200 OK with success confirmation
     */
    @DeleteMapping("/chatrooms/{roomId}")
    @Operation(summary = "Delete a chat room", description = "Deletes a chat room, removing all metadata, participants, and message history from Redis.")
    @ApiResponse(responseCode = "200", description = "Chat room deleted successfully",
            content = @Content(schema = @Schema(implementation = DeleteRoomResponse.class)))
    @ApiResponse(responseCode = "404", description = "Chat room not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<DeleteRoomResponse> deleteRoom(@PathVariable String roomId) {
        log.info("REST request to delete chat room '{}'", roomId);
        DeleteRoomResponse response = chatRoomService.deleteRoom(roomId);
        return ResponseEntity.ok(response);
    }
}
