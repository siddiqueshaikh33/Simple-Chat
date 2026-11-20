package com.example.chatapp.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.chatapp.dto.SendMessageRequest;
import com.example.chatapp.dto.SendMessageResponse;
import com.example.chatapp.exception.NotAParticipantException;
import com.example.chatapp.exception.RoomNotFoundException;
import com.example.chatapp.model.ChatMessage;
import com.example.chatapp.pubsub.MessagePublisher;
import com.example.chatapp.repository.ChatRoomRepository;
import com.example.chatapp.repository.MessageRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ChatMessageServiceTest {

    @Mock
    private ChatRoomRepository chatRoomRepository;

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private MessagePublisher messagePublisher;

    private Clock fixedClock;
    private ChatMessageService chatMessageService;

    private static final Instant FIXED_INSTANT = Instant.parse("2024-01-01T10:00:00Z");

    @BeforeEach
    void setUp() {
        fixedClock = Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC);
        chatMessageService = new ChatMessageService(
                chatRoomRepository,
                messageRepository,
                messagePublisher,
                fixedClock
        );
    }

    @Test
    @DisplayName("sendMessage with valid room saves to repository and publishes to topic")
    void sendMessage_withValidRoomAndPayload_savesToRepoAndPublishes() {
        when(chatRoomRepository.existsById("general")).thenReturn(true);
        when(chatRoomRepository.isParticipant("general", "guest_user")).thenReturn(true);
        SendMessageRequest request = new SendMessageRequest("guest_user", "Hello, everyone!");

        SendMessageResponse response = chatMessageService.sendMessage("General", request);

        assertThat(response.status()).isEqualTo("success");
        assertThat(response.message()).isEqualTo("Message sent successfully.");

        ChatMessage expectedMessage = new ChatMessage("guest_user", "Hello, everyone!", "2024-01-01T10:00:00Z");
        verify(messageRepository).saveMessage(eq("general"), eq(expectedMessage));
        verify(messagePublisher).publish(eq("general"), eq(expectedMessage));
    }

    @Test
    @DisplayName("sendMessage when publishing fails still returns success because message was persisted")
    void sendMessage_whenPublishingFails_stillSucceedsBecauseMessageIsSaved() {
        when(chatRoomRepository.existsById("general")).thenReturn(true);
        when(chatRoomRepository.isParticipant("general", "guest_user")).thenReturn(true);
        doThrow(new RuntimeException("Pub/Sub socket glitch"))
                .when(messagePublisher).publish(eq("general"), any());

        SendMessageRequest request = new SendMessageRequest("guest_user", "Hello, everyone!");
        SendMessageResponse response = chatMessageService.sendMessage("general", request);

        assertThat(response.status()).isEqualTo("success");
        assertThat(response.message()).isEqualTo("Message sent successfully.");

        ChatMessage expectedMessage = new ChatMessage("guest_user", "Hello, everyone!", "2024-01-01T10:00:00Z");
        verify(messageRepository).saveMessage(eq("general"), eq(expectedMessage));
    }

    @Test
    @DisplayName("sendMessage when sender is not a joined participant throws NotAParticipantException")
    void sendMessage_whenSenderNotParticipant_throwsNotAParticipantException() {
        when(chatRoomRepository.existsById("general")).thenReturn(true);
        when(chatRoomRepository.isParticipant("general", "stranger")).thenReturn(false);

        SendMessageRequest request = new SendMessageRequest("stranger", "Hello!");

        assertThatThrownBy(() -> chatMessageService.sendMessage("general", request))
                .isInstanceOf(NotAParticipantException.class)
                .hasMessage("User 'stranger' is not a participant of chat room 'general'.");
    }

    @Test
    @DisplayName("sendMessage when room not found throws RoomNotFoundException")
    void sendMessage_whenRoomNotFound_throwsRoomNotFoundException() {
        when(chatRoomRepository.existsById("missing")).thenReturn(false);
        SendMessageRequest request = new SendMessageRequest("guest_user", "Hello!");

        assertThatThrownBy(() -> chatMessageService.sendMessage("missing", request))
                .isInstanceOf(RoomNotFoundException.class)
                .hasMessage("Chat room 'missing' not found.");
    }

    @Test
    @DisplayName("sendMessage with null roomId throws IllegalArgumentException")
    void sendMessage_withNullRoomId_throwsIllegalArgumentException() {
        SendMessageRequest request = new SendMessageRequest("guest_user", "Hello!");

        assertThatThrownBy(() -> chatMessageService.sendMessage(null, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("roomId must not be null");
    }

    @Test
    @DisplayName("getRecentMessages with valid room returns list from repository")
    void getRecentMessages_withValidRoom_returnsMessagesFromRepo() {
        when(chatRoomRepository.existsById("general")).thenReturn(true);
        List<ChatMessage> expectedList = List.of(
                new ChatMessage("user1", "msg1", "2024-01-01T10:00:00Z"),
                new ChatMessage("user2", "msg2", "2024-01-01T10:01:00Z")
        );
        when(messageRepository.findRecentMessages("general", 10)).thenReturn(expectedList);

        List<ChatMessage> actual = chatMessageService.getRecentMessages("general", 10);

        assertThat(actual).hasSize(2).isEqualTo(expectedList);
        verify(messageRepository).findRecentMessages("general", 10);
    }

    @Test
    @DisplayName("getRecentMessages when room not found throws RoomNotFoundException")
    void getRecentMessages_whenRoomNotFound_throwsRoomNotFoundException() {
        when(chatRoomRepository.existsById("missing")).thenReturn(false);

        assertThatThrownBy(() -> chatMessageService.getRecentMessages("missing", 10))
                .isInstanceOf(RoomNotFoundException.class)
                .hasMessage("Chat room 'missing' not found.");
    }
}
