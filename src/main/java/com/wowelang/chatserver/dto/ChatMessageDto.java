package com.wowelang.chatserver.dto;

import java.time.Instant;

import com.wowelang.chatserver.model.ChatMessage.MessageType;

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
public class ChatMessageDto {
    
    private String id;
    
    private String roomId;
    
    private String senderId;
    
    @NotNull
    private MessageType type;
    
    @Size(max = 2000)
    private String content;
    
    private String s3Key;
    
    private String originalMessageId;
    
    private String correctedText;
    
    private Instant createdAt;
    
    private boolean deleted;
} 