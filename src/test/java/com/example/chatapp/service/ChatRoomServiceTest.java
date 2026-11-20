package com.example.chatapp.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.chatapp.dto.CreateRoomRequest;
import com.example.chatapp.dto.CreateRoomResponse;
import com.example.chatapp.dto.DeleteRoomResponse;
import com.example.chatapp.dto.JoinRoomRequest;
import com.example.chatapp.dto.JoinRoomResponse;
import com.example.chatapp.exception.RoomAlreadyExistsException;
import com.example.chatapp.exception.RoomNotFoundException;
import com.example.chatapp.repository.ChatRoomRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ChatRoomServiceTest {

    @Mock
    private ChatRoomRepository chatRoomRepository;

    private Clock fixedClock;
    private ChatRoomService chatRoomService;

    private static final Instant FIXED_INSTANT = Instant.parse("2024-01-01T10:00:00Z");

    @BeforeEach
    void setUp() {
        fixedClock = Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC);
        chatRoomService = new ChatRoomService(chatRoomRepository, fixedClock);
    }

    @Test
    @DisplayName("createRoom with valid name saves room and returns success response")
    void createRoom_withValidName_savesAndReturnsSuccessResponse() {
        CreateRoomRequest request = new CreateRoomRequest("General");

        CreateRoomResponse response = chatRoomService.createRoom(request);

        assertThat(response.status()).isEqualTo("success");
        assertThat(response.roomId()).isEqualTo("general");
        assertThat(response.message()).isEqualTo("Chat room 'general' created successfully.");

        verify(chatRoomRepository).saveRoom(eq("general"), eq("General"), eq(FIXED_INSTANT));
    }

    @Test
    @DisplayName("createRoom when duplicate name throws RoomAlreadyExistsException")
    void createRoom_withDuplicateName_throwsRoomAlreadyExistsException() {
        CreateRoomRequest request = new CreateRoomRequest("general");
        doThrow(new RoomAlreadyExistsException("general"))
                .when(chatRoomRepository).saveRoom(eq("general"), eq("general"), eq(FIXED_INSTANT));

        assertThatThrownBy(() -> chatRoomService.createRoom(request))
                .isInstanceOf(RoomAlreadyExistsException.class)
                .hasMessage("Chat room 'general' already exists.");
    }

    @Test
    @DisplayName("roomExists when room is present returns true")
    void roomExists_whenRoomPresent_returnsTrue() {
        when(chatRoomRepository.existsById("general")).thenReturn(true);

        boolean exists = chatRoomService.roomExists("General");

        assertThat(exists).isTrue();
        verify(chatRoomRepository).existsById("general");
    }

    @Test
    @DisplayName("roomExists when room is absent returns false")
    void roomExists_whenRoomAbsent_returnsFalse() {
        when(chatRoomRepository.existsById("missing")).thenReturn(false);

        boolean exists = chatRoomService.roomExists("missing");

        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("roomExists with null roomId returns false without querying repository")
    void roomExists_withNullRoomId_returnsFalse() {
        boolean exists = chatRoomService.roomExists(null);

        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("joinRoom with valid room and participant adds to set and returns success response")
    void joinRoom_withValidRoomAndParticipant_addsParticipantAndReturnsResponse() {
        when(chatRoomRepository.existsById("general")).thenReturn(true);
        JoinRoomRequest request = new JoinRoomRequest("guest_user");

        JoinRoomResponse response = chatRoomService.joinRoom("General", request);

        assertThat(response.status()).isEqualTo("success");
        assertThat(response.message()).isEqualTo("User 'guest_user' joined chat room 'general'.");
        verify(chatRoomRepository).addParticipant("general", "guest_user");
    }

    @Test
    @DisplayName("joinRoom when room does not exist throws RoomNotFoundException")
    void joinRoom_whenRoomNotFound_throwsRoomNotFoundException() {
        when(chatRoomRepository.existsById("missing")).thenReturn(false);
        JoinRoomRequest request = new JoinRoomRequest("guest_user");

        assertThatThrownBy(() -> chatRoomService.joinRoom("missing", request))
                .isInstanceOf(RoomNotFoundException.class)
                .hasMessage("Chat room 'missing' not found.");
    }

    @Test
    @DisplayName("deleteRoom when room exists deletes all keys and returns success response")
    void deleteRoom_whenRoomExists_deletesAndReturnsSuccessResponse() {
        when(chatRoomRepository.deleteRoom("general")).thenReturn(true);

        DeleteRoomResponse response = chatRoomService.deleteRoom("General");

        assertThat(response.status()).isEqualTo("success");
        assertThat(response.message()).isEqualTo("Chat room 'general' deleted successfully.");
        verify(chatRoomRepository).deleteRoom("general");
    }

    @Test
    @DisplayName("deleteRoom when room does not exist throws RoomNotFoundException")
    void deleteRoom_whenRoomNotFound_throwsRoomNotFoundException() {
        when(chatRoomRepository.deleteRoom("missing")).thenReturn(false);

        assertThatThrownBy(() -> chatRoomService.deleteRoom("missing"))
                .isInstanceOf(RoomNotFoundException.class)
                .hasMessage("Chat room 'missing' not found.");
    }
}
