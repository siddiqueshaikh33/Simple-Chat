package com.example.chatapp.service;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Service managing Server-Sent Events (SSE) subscribers for real-time room messaging.
 * Bridges Redis Pub/Sub events directly to HTTP client streams.
 */
@Service
public class SseEmitterService {

    private static final Logger log = LoggerFactory.getLogger(SseEmitterService.class);

    // Timeout: 30 minutes
    private static final Long SSE_TIMEOUT = 30 * 60 * 1000L;

    private final Map<String, List<SseEmitter>> emittersByRoom = new ConcurrentHashMap<>();

    /**
     * Registers a new SSE emitter for the specified chat room.
     *
     * @param roomId unique lowercase room identifier
     * @return configured SseEmitter
     */
    public SseEmitter register(String roomId) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);
        List<SseEmitter> emitters = emittersByRoom.computeIfAbsent(roomId, k -> new CopyOnWriteArrayList<>());
        emitters.add(emitter);

        Runnable cleanup = () -> {
            emitters.remove(emitter);
            if (emitters.isEmpty()) {
                emittersByRoom.remove(roomId, emitters);
            }
            log.debug("SSE emitter removed for room '{}'. Remaining listeners: {}", roomId, emitters.size());
        };

        emitter.onCompletion(cleanup);
        emitter.onTimeout(cleanup);
        emitter.onError(e -> cleanup.run());

        try {
            emitter.send(SseEmitter.event()
                    .name("connected")
                    .data("Connected to live real-time stream for chat room '" + roomId + "'"));
        } catch (IOException ex) {
            log.warn("Failed to send initial SSE handshake to room '{}': {}", roomId, ex.getMessage());
            cleanup.run();
        }

        log.info("Client connected to SSE stream for room '{}'. Total listeners: {}", roomId, emitters.size());
        return emitter;
    }

    /**
     * Broadcasts a message payload to all active SSE listeners subscribed to the room.
     *
     * @param roomId  unique room identifier
     * @param payload raw JSON message string
     */
    public void broadcast(String roomId, String payload) {
        List<SseEmitter> emitters = emittersByRoom.get(roomId);
        if (emitters == null || emitters.isEmpty()) {
            return;
        }

        log.info("Broadcasting real-time SSE message to {} listeners in room '{}'", emitters.size(), roomId);
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name("message")
                        .data(payload));
            } catch (Exception ex) {
                log.debug("Error sending SSE to subscriber in room '{}': {}", roomId, ex.getMessage());
                emitters.remove(emitter);
            }
        }
    }
}
