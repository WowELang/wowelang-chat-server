package com.wowelang.chatserver.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.core.mapping.event.BeforeConvertCallback;

import com.wowelang.chatserver.model.ChatMessage;
import com.wowelang.chatserver.model.ChatRoom;
import com.wowelang.chatserver.model.MatchRequest;

import java.time.Instant;

@Configuration
@EnableMongoAuditing
public class MongoConfig {

    @Bean
    public BeforeConvertCallback<ChatRoom> chatRoomBeforeConvertCallback() {
        return (entity, collection) -> {
            if (entity.getCreatedAt() == null) {
                entity.setCreatedAt(Instant.now());
            }
            entity.setUpdatedAt(Instant.now());
            return entity;
        };
    }

    @Bean
    public BeforeConvertCallback<ChatMessage> chatMessageBeforeConvertCallback() {
        return (entity, collection) -> {
            if (entity.getCreatedAt() == null) {
                entity.setCreatedAt(Instant.now());
            }
            return entity;
        };
    }

    @Bean
    public BeforeConvertCallback<MatchRequest> matchRequestBeforeConvertCallback() {
        return (entity, collection) -> {
            if (entity.getCreatedAt() == null) {
                entity.setCreatedAt(Instant.now());
            }
            entity.setUpdatedAt(Instant.now());
            return entity;
        };
    }
} 