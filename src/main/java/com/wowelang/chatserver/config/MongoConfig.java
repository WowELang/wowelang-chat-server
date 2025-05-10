package com.wowelang.chatserver.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.core.mapping.event.BeforeConvertCallback;
import org.springframework.data.mongodb.config.AbstractMongoClientConfiguration;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.connection.SslSettings;
import org.springframework.beans.factory.annotation.Value;

import com.wowelang.chatserver.model.ChatMessage;
import com.wowelang.chatserver.model.ChatRoom;
import com.wowelang.chatserver.model.MatchRequest;

import java.time.Instant;

@Configuration
@EnableMongoAuditing
public class MongoConfig extends AbstractMongoClientConfiguration {

    @Value("${spring.data.mongodb.uri}")
    private String mongoUri;

    @Value("${spring.data.mongodb.database:chatdb}")
    private String database;

    @Override
    protected String getDatabaseName() {
        return database;
    }

    @Override
    @Bean
    public MongoClientSettings mongoClientSettings() {
        ConnectionString connectionString = new ConnectionString(mongoUri);
        
        // 기본 설정 구성
        MongoClientSettings.Builder builder = MongoClientSettings.builder()
            .applyConnectionString(connectionString)
            // SSL 설정: SSL은 활성화하되 인증서 검증은 비활성화
            .applyToSslSettings(sslBuilder -> 
                sslBuilder.enabled(true)
                          .invalidHostNameAllowed(true));
        
        return builder.build();
    }

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