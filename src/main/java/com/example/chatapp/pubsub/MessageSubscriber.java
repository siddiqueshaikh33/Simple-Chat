package com.example.chatapp.pubsub;

import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

/**
 * Redis Pub/Sub subscriber that listens for real-time messages broadcast on chatroom channels.
 * Conforms to Slice 7 of AGENTS.md.
 */
@Component
public class MessageSubscriber implements MessageListener {

    private static final Logger log = LoggerFactory.getLogger(MessageSubscriber.class);

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String channel = new String(message.getChannel(), StandardCharsets.UTF_8);
        String body = new String(message.getBody(), StandardCharsets.UTF_8);
        log.info("Received real-time message on channel '{}': {}", channel, body);
    }
}
