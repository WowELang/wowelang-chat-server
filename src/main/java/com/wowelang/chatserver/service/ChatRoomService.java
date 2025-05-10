package com.wowelang.chatserver.service;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.wowelang.chatserver.dto.ChatRoomDto;
import com.wowelang.chatserver.dto.ChatMessageDto;
import com.wowelang.chatserver.exception.ResourceNotFoundException;
import com.wowelang.chatserver.model.ChatMessage;
import com.wowelang.chatserver.model.ChatRoom;
import com.wowelang.chatserver.repository.ChatMessageRepository;
import com.wowelang.chatserver.repository.ChatRoomRepository;
import com.wowelang.chatserver.util.UserContext;

import lombok.RequiredArgsConstructor;


// ChatRoomService.java

@Service
@RequiredArgsConstructor
public class ChatRoomService {
    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    
    // ChatMessageService 의존성 제거
    // private final ChatMessageService chatMessageService;
    
    public List<ChatRoomDto> getUserRooms() {
        String userId = UserContext.getUserId();
        List<ChatRoom> rooms = chatRoomRepository.findAllByParticipantId(userId);
        
        return rooms.stream()
                .map(room -> {
                    // 변경된 메서드 이름 사용
                    ChatMessage lastMessage = chatMessageRepository.findFirstByRoomIdAndDeletedFalseOrderByCreatedAtDesc(room.getId())
                            .orElse(null);
                    long unreadCount = chatMessageRepository.countByRoomIdAndDeletedFalse(room.getId());
                    
                    // ChatMessageService.convertToDto() 대신 내부 메서드 사용
                    return ChatRoomDto.builder()
                            .id(room.getId())
                            .participants(room.getParticipants())
                            .createdAt(room.getCreatedAt())
                            .updatedAt(room.getUpdatedAt())
                            .lastMessage(convertToChatMessageDto(lastMessage))
                            .unreadCount(unreadCount)
                            .build();
                })
                .collect(Collectors.toList());
    }

    // ChatMessageService.convertToDto() 대신 사용할 내부 메서드
    private ChatMessageDto convertToChatMessageDto(ChatMessage message) {
        if (message == null) {
            return null;
        }
        
        return ChatMessageDto.builder()
                .id(message.getId())
                .roomId(message.getRoomId())
                .senderId(message.getSenderId())
                .type(message.getType())
                .content(message.isDeleted() ? null : message.getContent())
                .s3Key(message.isDeleted() ? null : message.getS3Key())
                .originalMessageId(message.getOriginalMessageId())
                .createdAt(message.getCreatedAt())
                .deleted(message.isDeleted())
                .build();
    }
    public ChatRoom getRoom(String roomId) {
        return chatRoomRepository.findById(roomId)
                .filter(room -> !room.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Chat room not found"));
    }

    public ChatRoom createRoom(List<String> participants) {
        if (participants.size() != 2) {
            throw new IllegalArgumentException("Chat room must have exactly 2 participants");
        }

        // 이미 존재하는 채팅방인지 확인
        return chatRoomRepository.findByParticipants(participants.get(0), participants.get(1))
                .orElseGet(() -> {
                    ChatRoom newRoom = ChatRoom.builder()
                            .participants(participants)
                            .createdAt(Instant.now())
                            .updatedAt(Instant.now())
                            .deleted(false)
                            .build();
                    
                    return chatRoomRepository.save(newRoom);
                });
    }

    public void deleteRoom(String roomId) {
        String userId = UserContext.getUserId();
        
        ChatRoom room = getRoom(roomId);
        
        if (!room.getParticipants().contains(userId)) {
            throw new IllegalArgumentException("User is not a participant of this room");
        }
        
        room.setDeleted(true);
        room.setDeletedAt(Instant.now());
        chatRoomRepository.save(room);
    }
    
    public boolean isUserInRoom(String roomId, String userId) {
        ChatRoom room = getRoom(roomId);
        return room.getParticipants().contains(userId);
    }
} 