package com.example.chatapp.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.chatapp.dto.CreateRoomRequest;
import com.example.chatapp.dto.CreateRoomResponse;
import com.example.chatapp.dto.DeleteRoomResponse;
import com.example.chatapp.dto.JoinRoomRequest;
import com.example.chatapp.dto.JoinRoomResponse;
import com.example.chatapp.dto.SendMessageRequest;
import com.example.chatapp.dto.SendMessageResponse;
import com.example.chatapp.exception.GlobalExceptionHandler;
import com.example.chatapp.exception.NotAParticipantException;
import com.example.chatapp.exception.RoomAlreadyExistsException;
import com.example.chatapp.exception.RoomNotFoundException;
import com.example.chatapp.model.ChatMessage;
import com.example.chatapp.service.ChatMessageService;
import com.example.chatapp.service.ChatRoomService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ChatRoomController.class)
@Import(GlobalExceptionHandler.class)
class ChatRoomControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ChatRoomService chatRoomService;

    @MockBean
    private ChatMessageService chatMessageService;

    @Test
    @DisplayName("POST /api/chatapp/chatrooms with valid name returns 201 Created")
    void createRoom_withValidPayload_returns201Created() throws Exception {
        CreateRoomRequest request = new CreateRoomRequest("general");
        CreateRoomResponse response = CreateRoomResponse.of("general");

        when(chatRoomService.createRoom(any(CreateRoomRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/chatapp/chatrooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.roomId").value("general"))
                .andExpect(jsonPath("$.message").value("Chat room 'general' created successfully."));
    }

    @Test
    @DisplayName("POST /api/chatapp/chatrooms with duplicate name returns 409 Conflict")
    void createRoom_withDuplicateName_returns409Conflict() throws Exception {
        CreateRoomRequest request = new CreateRoomRequest("general");
        when(chatRoomService.createRoom(any(CreateRoomRequest.class)))
                .thenThrow(new RoomAlreadyExistsException("general"));

        mockMvc.perform(post("/api/chatapp/chatrooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value("Chat room 'general' already exists."));
    }

    @Test
    @DisplayName("POST /api/chatapp/chatrooms with blank name returns 400 Bad Request")
    void createRoom_withBlankName_returns400BadRequest() throws Exception {
        String invalidJson = "{\"roomName\": \"   \"}";

        mockMvc.perform(post("/api/chatapp/chatrooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.details").isArray());
    }

    @Test
    @DisplayName("POST /api/chatapp/chatrooms with special characters returns 400 Bad Request")
    void createRoom_withSpecialCharacters_returns400BadRequest() throws Exception {
        String invalidJson = "{\"roomName\": \"invalid!room@#\"}";

        mockMvc.perform(post("/api/chatapp/chatrooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.details").isArray());
    }

    @Test
    @DisplayName("POST /api/chatapp/chatrooms/{roomId}/join with valid participant returns 200 OK")
    void joinRoom_withValidParticipant_returns200Ok() throws Exception {
        JoinRoomRequest request = new JoinRoomRequest("guest_user");
        JoinRoomResponse response = JoinRoomResponse.of("guest_user", "general");

        when(chatRoomService.joinRoom(eq("general"), any(JoinRoomRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/chatapp/chatrooms/general/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("User 'guest_user' joined chat room 'general'."));
    }

    @Test
    @DisplayName("POST /api/chatapp/chatrooms/{roomId}/join with non-existent room returns 404 Not Found")
    void joinRoom_withMissingRoom_returns404NotFound() throws Exception {
        JoinRoomRequest request = new JoinRoomRequest("guest_user");
        when(chatRoomService.joinRoom(eq("missing"), any(JoinRoomRequest.class)))
                .thenThrow(new RoomNotFoundException("missing"));

        mockMvc.perform(post("/api/chatapp/chatrooms/missing/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value("Chat room 'missing' not found."));
    }

    @Test
    @DisplayName("POST /api/chatapp/chatrooms/{roomId}/join with blank participant returns 400 Bad Request")
    void joinRoom_withBlankParticipant_returns400BadRequest() throws Exception {
        String invalidJson = "{\"participant\": \"   \"}";

        mockMvc.perform(post("/api/chatapp/chatrooms/general/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.details").isArray());
    }

    @Test
    @DisplayName("POST /api/chatapp/chatrooms/{roomId}/messages with valid payload returns 200 OK")
    void sendMessage_withValidPayload_returns200Ok() throws Exception {
        SendMessageRequest request = new SendMessageRequest("guest_user", "Hello, everyone!");
        SendMessageResponse response = SendMessageResponse.ok();

        when(chatMessageService.sendMessage(eq("general"), any(SendMessageRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/chatapp/chatrooms/general/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Message sent successfully."));
    }

    @Test
    @DisplayName("POST /api/chatapp/chatrooms/{roomId}/messages with missing room returns 404 Not Found")
    void sendMessage_withMissingRoom_returns404NotFound() throws Exception {
        SendMessageRequest request = new SendMessageRequest("guest_user", "Hello!");
        when(chatMessageService.sendMessage(eq("missing"), any(SendMessageRequest.class)))
                .thenThrow(new RoomNotFoundException("missing"));

        mockMvc.perform(post("/api/chatapp/chatrooms/missing/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value("Chat room 'missing' not found."));
    }

    @Test
    @DisplayName("POST /api/chatapp/chatrooms/{roomId}/messages with non-participant returns 403 Forbidden")
    void sendMessage_withNonParticipant_returns403Forbidden() throws Exception {
        SendMessageRequest request = new SendMessageRequest("stranger", "Hello!");
        when(chatMessageService.sendMessage(eq("general"), any(SendMessageRequest.class)))
                .thenThrow(new NotAParticipantException("stranger", "general"));

        mockMvc.perform(post("/api/chatapp/chatrooms/general/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value("User 'stranger' is not a participant of chat room 'general'."));
    }

    @Test
    @DisplayName("POST /api/chatapp/chatrooms/{roomId}/messages with blank participant returns 400 Bad Request")
    void sendMessage_withBlankParticipant_returns400BadRequest() throws Exception {
        String invalidJson = "{\"participant\": \"   \", \"message\": \"Hello!\"}";

        mockMvc.perform(post("/api/chatapp/chatrooms/general/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.details").isArray());
    }

    @Test
    @DisplayName("POST /api/chatapp/chatrooms/{roomId}/messages with blank message returns 400 Bad Request")
    void sendMessage_withBlankMessage_returns400BadRequest() throws Exception {
        String invalidJson = "{\"participant\": \"guest_user\", \"message\": \"   \"}";

        mockMvc.perform(post("/api/chatapp/chatrooms/general/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.details").isArray());
    }

    @Test
    @DisplayName("GET /api/chatapp/chatrooms/{roomId}/messages returns 200 OK with message history (default limit 10)")
    void getMessages_whenRoomExists_returns200AndMessageList() throws Exception {
        List<ChatMessage> messages = List.of(
                new ChatMessage("user1", "First message", "2024-01-01T10:00:00Z"),
                new ChatMessage("user2", "Second message", "2024-01-01T10:01:00Z")
        );
        when(chatMessageService.getRecentMessages("general", 10)).thenReturn(messages);

        mockMvc.perform(get("/api/chatapp/chatrooms/general/messages"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.messages").isArray())
                .andExpect(jsonPath("$.messages.length()").value(2))
                .andExpect(jsonPath("$.messages[0].participant").value("user1"))
                .andExpect(jsonPath("$.messages[0].message").value("First message"))
                .andExpect(jsonPath("$.messages[0].timestamp").value("2024-01-01T10:00:00Z"))
                .andExpect(jsonPath("$.messages[1].participant").value("user2"))
                .andExpect(jsonPath("$.messages[1].message").value("Second message"));
    }

    @Test
    @DisplayName("GET /api/chatapp/chatrooms/{roomId}/messages with custom limit passes limit to service")
    void getMessages_withCustomLimit_passesLimitToService() throws Exception {
        List<ChatMessage> messages = List.of(
                new ChatMessage("user1", "Latest message", "2024-01-01T10:00:00Z")
        );
        when(chatMessageService.getRecentMessages("general", 5)).thenReturn(messages);

        mockMvc.perform(get("/api/chatapp/chatrooms/general/messages").param("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.messages").isArray())
                .andExpect(jsonPath("$.messages.length()").value(1));
    }

    @Test
    @DisplayName("GET /api/chatapp/chatrooms/{roomId}/messages with limit <= 0 returns 400 Bad Request")
    void getMessages_whenLimitIsZeroOrNegative_returns400() throws Exception {
        mockMvc.perform(get("/api/chatapp/chatrooms/general/messages").param("limit", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value("Limit must be between 1 and 100."));

        mockMvc.perform(get("/api/chatapp/chatrooms/general/messages").param("limit", "-5"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value("Limit must be between 1 and 100."));
    }

    @Test
    @DisplayName("GET /api/chatapp/chatrooms/{roomId}/messages with limit > 100 returns 400 Bad Request")
    void getMessages_whenLimitExceeds100_returns400() throws Exception {
        mockMvc.perform(get("/api/chatapp/chatrooms/general/messages").param("limit", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value("Limit must be between 1 and 100."));
    }

    @Test
    @DisplayName("GET /api/chatapp/chatrooms/{roomId}/messages with missing room returns 404 Not Found")
    void getMessages_whenRoomNotFound_returns404NotFound() throws Exception {
        when(chatMessageService.getRecentMessages("missing", 10))
                .thenThrow(new RoomNotFoundException("missing"));

        mockMvc.perform(get("/api/chatapp/chatrooms/missing/messages"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value("Chat room 'missing' not found."));
    }

    @Test
    @DisplayName("DELETE /api/chatapp/chatrooms/{roomId} returns 200 OK when room is deleted")
    void deleteRoom_whenRoomExists_returns200Ok() throws Exception {
        when(chatRoomService.deleteRoom("general"))
                .thenReturn(DeleteRoomResponse.of("general"));

        mockMvc.perform(delete("/api/chatapp/chatrooms/general"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Chat room 'general' deleted successfully."));
    }

    @Test
    @DisplayName("DELETE /api/chatapp/chatrooms/{roomId} returns 404 Not Found when room does not exist")
    void deleteRoom_whenRoomNotFound_returns404NotFound() throws Exception {
        when(chatRoomService.deleteRoom("missing"))
                .thenThrow(new RoomNotFoundException("missing"));

        mockMvc.perform(delete("/api/chatapp/chatrooms/missing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value("Chat room 'missing' not found."));
    }
}
