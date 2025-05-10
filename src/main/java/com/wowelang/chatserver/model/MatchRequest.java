package com.wowelang.chatserver.model;

import java.time.Instant;

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
@Document(collection = "match_requests")
@CompoundIndex(name = "requester_target_idx", def = "{'requesterId': 1, 'targetId': 1}", unique = true)
public class MatchRequest {
    
    @Id
    private String id;
    
    private String requesterId;
    
    private String targetId;
    
    private MatchStatus status;
    
    private Instant createdAt;
    
    private Instant updatedAt;
    
    private String chatRoomId; // 매치가 수락되었을 때 생성된 채팅방 ID
    
    public enum MatchStatus {
        PENDING, ACCEPTED, REJECTED
    }
} 