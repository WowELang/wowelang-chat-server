package com.wowelang.chatserver.model;

import java.time.Instant;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "chat_rooms")
@CompoundIndex(name = "participants_idx", def = "{'participants': 1}")
public class ChatRoom {
    
    @Id
    private String id;
    
    private List<String> participants; // 정확히 2명의 참여자
    
    private Instant createdAt;
    
    private Instant updatedAt;
    
    @Builder.Default
    private boolean deleted = false;
    
    private Instant deletedAt;
} 