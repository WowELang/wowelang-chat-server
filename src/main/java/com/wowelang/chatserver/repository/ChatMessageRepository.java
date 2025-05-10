package com.wowelang.chatserver.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import com.wowelang.chatserver.model.ChatMessage;

public interface ChatMessageRepository extends MongoRepository<ChatMessage, String> {
    
    @Query("{ 'roomId': ?0, 'deleted': false, 'createdAt': { $lt: ?1 } }")
    List<ChatMessage> findByRoomIdAndCreatedAtLessThanOrderByCreatedAtDesc(String roomId, Instant before, Pageable pageable);
    
    @Query("{ 'roomId': ?0, 'deleted': false }")
    List<ChatMessage> findByRoomIdOrderByCreatedAtDesc(String roomId, Pageable pageable);
    
    Optional<ChatMessage> findFirstByRoomIdAndDeletedFalseOrderByCreatedAtDesc(String roomId);
    
    @Query(value = "{ 'roomId': ?0, 'deleted': false }", count = true)
    long countByRoomIdAndDeletedFalse(String roomId);
} 