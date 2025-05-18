package com.wowelang.chatserver.model;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "chat_messages")
public class ChatMessage {
    
    @Id
    private String id;
    
    @NotBlank
    @Indexed
    private String roomId;
    
    @NotBlank
    private String senderId;
    
    @NotNull
    private MessageType type;
    
    @Size(max = 2000)
    private String content;
    
    private String s3Key; // 이미지 메시지용
    
    private String originalMessage; // 교정 메시지용 원본 텍스트
    
    private Instant createdAt;
    
    @Builder.Default
    private boolean deleted = false;
    
    private Instant deletedAt;
    
    public enum MessageType {
        TEXT, IMAGE, CORRECTION
    }
} 