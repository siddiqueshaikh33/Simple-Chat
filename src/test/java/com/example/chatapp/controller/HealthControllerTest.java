package com.example.chatapp.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.chatapp.exception.GlobalExceptionHandler;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(HealthController.class)
@Import({GlobalExceptionHandler.class, HealthControllerTest.TestClockConfig.class})
class HealthControllerTest {

    @TestConfiguration
    static class TestClockConfig {
        @Bean
        public Clock clock() {
            return Clock.fixed(Instant.parse("2026-11-21T03:00:00Z"), ZoneOffset.UTC);
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StringRedisTemplate stringRedisTemplate;

    @Test
    @DisplayName("GET / returns 200 OK with service info and docs link")
    void root_returns200OkWithServiceInfo() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.service").value("Simple Chat Backend API"))
                .andExpect(jsonPath("$.status").value("RUNNING"))
                .andExpect(jsonPath("$.apiDocs").value("/swagger-ui.html"))
                .andExpect(jsonPath("$.healthCheck").value("/health"));
    }

    @Test
    @DisplayName("GET /health returns 200 OK and UP when Redis responds with PONG")
    void health_whenRedisHealthy_returns200OkAndUp() throws Exception {
        when(stringRedisTemplate.execute(any(RedisCallback.class))).thenReturn("PONG");

        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.redis").value("UP"))
                .andExpect(jsonPath("$.timestamp").value("2026-11-21T03:00:00Z"))
                .andExpect(jsonPath("$.error").doesNotExist());
    }

    @Test
    @DisplayName("GET /api/chatapp/health returns 200 OK and UP when Redis is healthy")
    void apiChatAppHealth_whenRedisHealthy_returns200OkAndUp() throws Exception {
        when(stringRedisTemplate.execute(any(RedisCallback.class))).thenReturn("PONG");

        mockMvc.perform(get("/api/chatapp/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.redis").value("UP"));
    }

    @Test
    @DisplayName("GET /health returns 503 Service Unavailable when Redis connection fails")
    void health_whenRedisFails_returns503ServiceUnavailable() throws Exception {
        when(stringRedisTemplate.execute(any(RedisCallback.class)))
                .thenThrow(new RedisConnectionFailureException("Unable to connect to Redis on localhost:6379"));

        mockMvc.perform(get("/health"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value("DOWN"))
                .andExpect(jsonPath("$.redis").value("DOWN"))
                .andExpect(jsonPath("$.error").value("Redis connection failed: Unable to connect to Redis on localhost:6379"));
    }

    @Test
    @DisplayName("GET /health returns 503 Service Unavailable when Redis ping response is not PONG")
    void health_whenRedisPingNotPong_returns503ServiceUnavailable() throws Exception {
        when(stringRedisTemplate.execute(any(RedisCallback.class))).thenReturn("UNEXPECTED");

        mockMvc.perform(get("/health"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value("DOWN"))
                .andExpect(jsonPath("$.redis").value("DOWN"));
    }
}
