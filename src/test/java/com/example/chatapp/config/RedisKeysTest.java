package com.example.chatapp.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RedisKeysTest {

    @Test
    @DisplayName("roomMetadataKey with valid room id returns lowercase formatted key")
    void roomMetadataKey_withValidRoomId_returnsFormattedKey() {
        String key = RedisKeys.roomMetadataKey("General");
        assertThat(key).isEqualTo("chatroom:general");
    }

    @Test
    @DisplayName("participantsKey with valid room id returns lowercase formatted key")
    void participantsKey_withValidRoomId_returnsFormattedKey() {
        String key = RedisKeys.participantsKey("  Tech_Room  ");
        assertThat(key).isEqualTo("chatroom:tech_room:participants");
    }

    @Test
    @DisplayName("messagesKey with valid room id returns lowercase formatted key")
    void messagesKey_withValidRoomId_returnsFormattedKey() {
        String key = RedisKeys.messagesKey("general");
        assertThat(key).isEqualTo("chatroom:general:messages");
    }

    @Test
    @DisplayName("channelTopic with valid room id returns lowercase formatted key")
    void channelTopic_withValidRoomId_returnsFormattedKey() {
        String key = RedisKeys.channelTopic("general");
        assertThat(key).isEqualTo("chatroom:general:channel");
    }

    @Test
    @DisplayName("methods throw IllegalArgumentException when roomId is null")
    void keys_withNullRoomId_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> RedisKeys.roomMetadataKey(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("roomId must not be null");
    }

    @Test
    @DisplayName("all channels pattern is chatroom:*:channel")
    void allChannelsPattern_returnsExpectedWildcard() {
        assertThat(RedisKeys.CHATROOM_CHANNEL_PATTERN).isEqualTo("chatroom:*:channel");
    }

    @Test
    @DisplayName("extractRoomIdFromChannel correctly parses roomId")
    void extractRoomIdFromChannel_returnsRoomId() {
        assertThat(RedisKeys.extractRoomIdFromChannel("chatroom:tech:channel")).isEqualTo("tech");
        assertThat(RedisKeys.extractRoomIdFromChannel("chatroom:general:channel")).isEqualTo("general");
        assertThat(RedisKeys.extractRoomIdFromChannel("invalid:channel")).isNull();
        assertThat(RedisKeys.extractRoomIdFromChannel(null)).isNull();
    }
}
