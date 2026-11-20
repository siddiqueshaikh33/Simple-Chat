package com.example.chatapp.config;

import com.example.chatapp.pubsub.MessageSubscriber;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

/**
 * Spring configuration providing primary Redis templates and utility beans.
 * Adheres to Dependency Inversion by providing injectable abstractions.
 */
@Configuration
public class RedisConfig {

    /**
     * Configures a StringRedisTemplate using the auto-configured Lettuce connection factory.
     * Guarantees all keys and values are serialized as clean, human-readable UTF-8 strings.
     *
     * @param connectionFactory Lettuce Redis connection factory configured from application.properties
     * @return configured StringRedisTemplate instance
     */
    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        return new StringRedisTemplate(connectionFactory);
    }

    /**
     * Configures the RedisMessageListenerContainer to manage subscriptions for real-time Pub/Sub channels.
     * Listens to the wildcard pattern "chatroom:*:channel".
     *
     * @param connectionFactory Lettuce Redis connection factory
     * @param messageSubscriber the component handling incoming Redis Pub/Sub messages
     * @return configured RedisMessageListenerContainer
     */
    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory,
            MessageSubscriber messageSubscriber) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(messageSubscriber, new PatternTopic(RedisKeys.CHATROOM_CHANNEL_PATTERN));
        return container;
    }

    /**
     * Provides an injectable UTC Clock for deterministic and testable timestamp generation.
     * Services inject this Clock instead of calling static Instant.now(), ensuring full unit testability.
     *
     * @return UTC Clock bean
     */
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
