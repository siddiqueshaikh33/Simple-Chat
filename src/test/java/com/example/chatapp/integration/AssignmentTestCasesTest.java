package com.example.chatapp.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.chatapp.dto.CreateRoomRequest;
import com.example.chatapp.dto.CreateRoomResponse;
import com.example.chatapp.dto.DeleteRoomResponse;
import com.example.chatapp.dto.JoinRoomRequest;
import com.example.chatapp.dto.JoinRoomResponse;
import com.example.chatapp.dto.SendMessageRequest;
import com.example.chatapp.dto.SendMessageResponse;
import com.example.chatapp.exception.RoomAlreadyExistsException;
import com.example.chatapp.exception.RoomNotFoundException;
import com.example.chatapp.model.ChatMessage;
import com.example.chatapp.pubsub.MessagePublisher;
import com.example.chatapp.pubsub.MessageSubscriber;
import com.example.chatapp.repository.ChatRoomRepository;
import com.example.chatapp.repository.MessageRepository;
import com.example.chatapp.service.ChatMessageService;
import com.example.chatapp.service.ChatRoomService;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.data.redis.connection.DefaultMessage;

/**
 * Verification of the exact 5 Test Cases specified on Pages 4 & 5 of the assignment PDF:
 * <ol>
 *   <li>Test Case 1: Create and Join Chat Room</li>
 *   <li>Test Case 2: Send and Retrieve Messages</li>
 *   <li>Test Case 3: Real-Time Messaging</li>
 *   <li>Test Case 4: Error Handling</li>
 *   <li>Test Case 5: Chat Room Deletion (Optional)</li>
 * </ol>
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AssignmentTestCasesTest {

    private InMemoryChatRoomRepository chatRoomRepository;
    private InMemoryMessageRepository messageRepository;
    private ChatRoomService chatRoomService;
    private ChatMessageService chatMessageService;
    private MessageSubscriber messageSubscriber;
    private AtomicReference<String> publishedChannel;
    private AtomicReference<String> publishedPayload;

    private static final Instant FIXED_INSTANT_1 = Instant.parse("2024-01-01T10:00:00Z");
    private static final Instant FIXED_INSTANT_2 = Instant.parse("2024-01-01T10:01:00Z");

    @BeforeEach
    void setUp() {
        messageRepository = new InMemoryMessageRepository();
        chatRoomRepository = new InMemoryChatRoomRepository(messageRepository);
        messageSubscriber = new MessageSubscriber();

        publishedChannel = new AtomicReference<>();
        publishedPayload = new AtomicReference<>();

        Clock clock = Clock.fixed(FIXED_INSTANT_1, ZoneOffset.UTC);

        chatRoomService = new ChatRoomService(chatRoomRepository, clock);

        MessagePublisher messagePublisher = new MessagePublisher(null, null) {
            @Override
            public void publish(String roomId, ChatMessage message) {
                String channel = "chatroom:" + roomId + ":channel";
                String payload = String.format("{\"participant\":\"%s\",\"message\":\"%s\",\"timestamp\":\"%s\"}",
                        message.participant(), message.message(), message.timestamp());
                publishedChannel.set(channel);
                publishedPayload.set(payload);

                // Broadcast directly to subscriber (simulating Redis Pub/Sub delivery)
                messageSubscriber.onMessage(
                        new DefaultMessage(channel.getBytes(StandardCharsets.UTF_8), payload.getBytes(StandardCharsets.UTF_8)),
                        "chatroom:*:channel".getBytes(StandardCharsets.UTF_8)
                );
            }
        };

        chatMessageService = new ChatMessageService(chatRoomRepository, messageRepository, messagePublisher, clock);
    }

    /**
     * Test Case 1: Create and Join Chat Room
     * Input: Create a chat room general and join as guest_user.
     * Expected Output: Room metadata is stored, and guest_user is added to the Redis Set.
     */
    @Test
    @Order(1)
    @DisplayName("Test Case 1: Create and Join Chat Room")
    void testCase1_createAndJoinChatRoom() {
        // 1. Input: Create a chat room "general"
        CreateRoomRequest createRequest = new CreateRoomRequest("general");
        CreateRoomResponse createResponse = chatRoomService.createRoom(createRequest);

        assertThat(createResponse.status()).isEqualTo("success");
        assertThat(createResponse.roomId()).isEqualTo("general");
        assertThat(createResponse.message()).isEqualTo("Chat room 'general' created successfully.");

        // Expected Output 1: Room metadata is stored in Redis Hash
        Optional<Map<String, String>> metadataOpt = chatRoomRepository.findMetadataById("general");
        assertThat(metadataOpt).isPresent();
        Map<String, String> metadata = metadataOpt.get();
        assertThat(metadata.get("roomId")).isEqualTo("general");
        assertThat(metadata.get("roomName")).isEqualTo("general");
        assertThat(metadata.get("createdAt")).isEqualTo("2024-01-01T10:00:00Z");

        // 2. Input: Join as "guest_user"
        JoinRoomRequest joinRequest = new JoinRoomRequest("guest_user");
        JoinRoomResponse joinResponse = chatRoomService.joinRoom("general", joinRequest);

        assertThat(joinResponse.status()).isEqualTo("success");
        assertThat(joinResponse.message()).isEqualTo("User 'guest_user' joined chat room 'general'.");

        // Expected Output 2: guest_user is added to the Redis Set
        boolean isParticipant = chatRoomRepository.isParticipant("general", "guest_user");
        assertThat(isParticipant).isTrue();
        assertThat(chatRoomRepository.getParticipants("general")).contains("guest_user");
    }

    /**
     * Test Case 2: Send and Retrieve Messages
     * Input: Send two messages in a chat room and retrieve the last message.
     * Expected Output: Messages appear in the correct order.
     */
    @Test
    @Order(2)
    @DisplayName("Test Case 2: Send and Retrieve Messages")
    void testCase2_sendAndRetrieveMessages() {
        // Setup room and participant
        chatRoomService.createRoom(new CreateRoomRequest("general"));
        chatRoomService.joinRoom("general", new JoinRoomRequest("guest_user"));

        // Input 1: Send first message
        SendMessageRequest msg1 = new SendMessageRequest("guest_user", "Hello, everyone!");
        SendMessageResponse resp1 = chatMessageService.sendMessage("general", msg1);
        assertThat(resp1.status()).isEqualTo("success");

        // Input 2: Send second message
        SendMessageRequest msg2 = new SendMessageRequest("guest_user", "How is everyone doing?");
        SendMessageResponse resp2 = chatMessageService.sendMessage("general", msg2);
        assertThat(resp2.status()).isEqualTo("success");

        // Input 3: Retrieve the last message (limit = 1)
        List<ChatMessage> lastMessageList = chatMessageService.getRecentMessages("general", 1);

        // Expected Output: Retrieve the last message returns the most recent message
        assertThat(lastMessageList).hasSize(1);
        assertThat(lastMessageList.get(0).message()).isEqualTo("How is everyone doing?");
        assertThat(lastMessageList.get(0).participant()).isEqualTo("guest_user");

        // Retrieve both messages (limit = 2) to verify chronological order (oldest to newest)
        List<ChatMessage> bothMessages = chatMessageService.getRecentMessages("general", 2);
        assertThat(bothMessages).hasSize(2);
        assertThat(bothMessages.get(0).message()).isEqualTo("Hello, everyone!");
        assertThat(bothMessages.get(1).message()).isEqualTo("How is everyone doing?");
    }

    /**
     * Test Case 3: Real-Time Messaging
     * Input: Publish a message to the room's Pub/Sub channel.
     * Expected Output: All participants subscribed to the room receive the message.
     */
    @Test
    @Order(3)
    @DisplayName("Test Case 3: Real-Time Messaging")
    void testCase3_realTimeMessaging() {
        // Setup room and participant
        chatRoomService.createRoom(new CreateRoomRequest("general"));
        chatRoomService.joinRoom("general", new JoinRoomRequest("guest_user"));

        // Input: Publish a message
        SendMessageRequest request = new SendMessageRequest("guest_user", "Live real-time message!");
        chatMessageService.sendMessage("general", request);

        // Expected Output: Message was published to room's channel and delivered to subscriber
        assertThat(publishedChannel.get()).isEqualTo("chatroom:general:channel");
        assertThat(publishedPayload.get()).contains("Live real-time message!");
        assertThat(publishedPayload.get()).contains("guest_user");
    }

    /**
     * Test Case 4: Error Handling
     * Input: Attempt to send a message to a non-existent chat room.
     * Expected Output: Error response indicating invalid room.
     */
    @Test
    @Order(4)
    @DisplayName("Test Case 4: Error Handling")
    void testCase4_errorHandling() {
        // Input: Attempt to send a message to a non-existent room "non_existent_room"
        SendMessageRequest request = new SendMessageRequest("guest_user", "Hello to empty room");

        // Expected Output: Throws RoomNotFoundException mapping to "Chat room 'non_existent_room' not found."
        assertThatThrownBy(() -> chatMessageService.sendMessage("non_existent_room", request))
                .isInstanceOf(RoomNotFoundException.class)
                .hasMessage("Chat room 'non_existent_room' not found.");
    }

    /**
     * Test Case 5: Chat Room Deletion (Optional)
     * Input: Delete a chat room.
     * Expected Output: Redis keys related to the room are removed.
     */
    @Test
    @Order(5)
    @DisplayName("Test Case 5: Chat Room Deletion")
    void testCase5_chatRoomDeletion() {
        // Setup: Create room, join, and post message
        chatRoomService.createRoom(new CreateRoomRequest("general"));
        chatRoomService.joinRoom("general", new JoinRoomRequest("guest_user"));
        chatMessageService.sendMessage("general", new SendMessageRequest("guest_user", "Test message"));

        // Verify keys exist before deletion
        assertThat(chatRoomRepository.existsById("general")).isTrue();
        assertThat(chatRoomRepository.findMetadataById("general")).isPresent();
        assertThat(chatRoomRepository.isParticipant("general", "guest_user")).isTrue();
        assertThat(messageRepository.findRecentMessages("general", 10)).isNotEmpty();

        // Input: Delete the chat room "general"
        DeleteRoomResponse deleteResponse = chatRoomService.deleteRoom("general");
        assertThat(deleteResponse.status()).isEqualTo("success");
        assertThat(deleteResponse.message()).isEqualTo("Chat room 'general' deleted successfully.");

        // Expected Output: All Redis keys related to the room are removed
        assertThat(chatRoomRepository.existsById("general")).isFalse();
        assertThat(chatRoomRepository.findMetadataById("general")).isEmpty();
        assertThat(chatRoomRepository.isParticipant("general", "guest_user")).isFalse();
        assertThat(chatRoomRepository.getParticipants("general")).isEmpty();
        assertThat(messageRepository.findRecentMessages("general", 10)).isEmpty();
    }

    // --- In-Memory Repository Implementations simulating Redis Hash, Set, and List ---

    private static class InMemoryChatRoomRepository implements ChatRoomRepository {
        private final Set<String> registeredRooms = new HashSet<>();
        private final Map<String, Map<String, String>> metadataHash = new HashMap<>();
        private final Map<String, Set<String>> participantsSet = new HashMap<>();
        private final InMemoryMessageRepository messageRepo;

        public InMemoryChatRoomRepository(InMemoryMessageRepository messageRepo) {
            this.messageRepo = messageRepo;
        }

        @Override
        public void saveRoom(String roomId, String roomName, Instant createdAt) {
            if (registeredRooms.contains(roomId)) {
                throw new RoomAlreadyExistsException(roomId);
            }
            registeredRooms.add(roomId);
            Map<String, String> meta = new HashMap<>();
            meta.put("roomId", roomId);
            meta.put("roomName", roomName);
            meta.put("createdAt", createdAt.toString());
            metadataHash.put(roomId, meta);
            participantsSet.put(roomId, new HashSet<>());
        }

        @Override
        public boolean existsById(String roomId) {
            return registeredRooms.contains(roomId);
        }

        @Override
        public Optional<Map<String, String>> findMetadataById(String roomId) {
            return Optional.ofNullable(metadataHash.get(roomId));
        }

        @Override
        public boolean addParticipant(String roomId, String participant) {
            return participantsSet.computeIfAbsent(roomId, k -> new HashSet<>()).add(participant);
        }

        @Override
        public boolean isParticipant(String roomId, String participant) {
            Set<String> set = participantsSet.get(roomId);
            return set != null && set.contains(participant);
        }

        @Override
        public Set<String> getParticipants(String roomId) {
            return participantsSet.getOrDefault(roomId, Collections.emptySet());
        }

        @Override
        public boolean deleteRoom(String roomId) {
            if (!registeredRooms.contains(roomId)) {
                return false;
            }
            registeredRooms.remove(roomId);
            metadataHash.remove(roomId);
            participantsSet.remove(roomId);
            messageRepo.deleteMessages(roomId);
            return true;
        }
    }

    private static class InMemoryMessageRepository implements MessageRepository {
        private final Map<String, List<ChatMessage>> messagesList = new HashMap<>();

        public void deleteMessages(String roomId) {
            messagesList.remove(roomId);
        }

        @Override
        public void saveMessage(String roomId, ChatMessage message) {
            messagesList.computeIfAbsent(roomId, k -> new ArrayList<>()).add(message);
        }

        @Override
        public List<ChatMessage> findRecentMessages(String roomId, int limit) {
            List<ChatMessage> list = messagesList.getOrDefault(roomId, Collections.emptyList());
            if (list.isEmpty()) {
                return Collections.emptyList();
            }
            int start = Math.max(0, list.size() - limit);
            return new ArrayList<>(list.subList(start, list.size()));
        }
    }
}
