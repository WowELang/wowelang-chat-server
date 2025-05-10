package com.wowelang.chatserver.dto;

import java.time.Instant;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRoomDto {
    
    private String id;
    
    private List<String> participants;
    
    private Instant createdAt;
    
    private Instant updatedAt;
    
    private ChatMessageDto lastMessage;
    
    private long unreadCount;
} 