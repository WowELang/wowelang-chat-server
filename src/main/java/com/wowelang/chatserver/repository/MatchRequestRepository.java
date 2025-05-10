package com.wowelang.chatserver.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import com.wowelang.chatserver.model.MatchRequest;
import com.wowelang.chatserver.model.MatchRequest.MatchStatus;

public interface MatchRequestRepository extends MongoRepository<MatchRequest, String> {
    
    @Query("{ 'targetId': ?0, 'status': ?1 }")
    List<MatchRequest> findByTargetIdAndStatus(String targetId, MatchStatus status);
    
    @Query("{ 'requesterId': ?0, 'status': ?1 }")
    List<MatchRequest> findByRequesterIdAndStatus(String requesterId, MatchStatus status);
    
    @Query("{ $or: [ { 'requesterId': ?0 }, { 'targetId': ?0 } ], 'status': ?1 }")
    List<MatchRequest> findByUserIdAndStatus(String userId, MatchStatus status);
    
    Optional<MatchRequest> findByRequesterIdAndTargetId(String requesterId, String targetId);
} 