package com.example.chatapp.pubsub;

import static org.assertj.core.api.Assertions.assertThatCode;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.DefaultMessage;

class MessageSubscriberTest {

    @Test
    @DisplayName("onMessage extracts channel and body without throwing any exception")
    void onMessage_receivesAndProcessesMessageSuccessfully() {
        MessageSubscriber subscriber = new MessageSubscriber();

        byte[] channel = "chatroom:general:channel".getBytes(StandardCharsets.UTF_8);
        byte[] body = "{\"participant\":\"alice\",\"message\":\"hello\",\"timestamp\":\"2024-01-01T10:00:00Z\"}".getBytes(StandardCharsets.UTF_8);
        byte[] pattern = "chatroom:*:channel".getBytes(StandardCharsets.UTF_8);

        DefaultMessage message = new DefaultMessage(channel, body);

        assertThatCode(() -> subscriber.onMessage(message, pattern))
                .doesNotThrowAnyException();
    }
}
