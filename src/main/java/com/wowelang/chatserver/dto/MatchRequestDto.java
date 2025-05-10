package com.wowelang.chatserver.dto;

import java.time.Instant;

import com.wowelang.chatserver.model.MatchRequest.MatchStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchRequestDto {
    
    private String id;
    
    private String requesterId;
    
    private String targetId;
    
    private MatchStatus status;
    
    private Instant createdAt;
    
    private Instant updatedAt;
    
    private String chatRoomId;
} 