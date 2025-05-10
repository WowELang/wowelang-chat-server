package com.wowelang.chatserver.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import com.wowelang.chatserver.model.ChatRoom;

public interface ChatRoomRepository extends MongoRepository<ChatRoom, String> {
    
    @Query("{ 'participants': ?0, 'deleted': false }")
    List<ChatRoom> findAllByParticipantId(String userId);
    
    @Query("{ 'participants': { $all: [?0, ?1] }, 'deleted': false }")
    Optional<ChatRoom> findByParticipants(String userId1, String userId2);
    
    boolean existsByParticipantsContainingAndDeletedFalse(String userId);
} 