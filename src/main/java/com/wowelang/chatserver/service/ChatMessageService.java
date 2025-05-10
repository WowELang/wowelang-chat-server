package com.wowelang.chatserver.service;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;

import com.wowelang.chatserver.dto.ChatMessageDto;
import com.wowelang.chatserver.exception.ResourceNotFoundException;
import com.wowelang.chatserver.model.ChatMessage;
import com.wowelang.chatserver.model.ChatMessage.MessageType;
import com.wowelang.chatserver.model.ChatRoom;
import com.wowelang.chatserver.repository.ChatMessageRepository;
import com.wowelang.chatserver.util.UserContext;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ChatMessageService {

    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomService chatRoomService;
    private final SimpMessageSendingOperations messagingTemplate;

    @Value("${chat.pagination.default-size}")
    private int defaultPageSize;

    @Value("${chat.pagination.max-size}")
    private int maxPageSize;

    @Value("${chat.message.max-text-length}")
    private int maxTextLength;

    public ChatMessageDto sendMessage(ChatMessageDto messageDto) {
        String userId = UserContext.getUserId();
        ChatRoom room = chatRoomService.getRoom(messageDto.getRoomId());
        
        // 채팅방 참여자인지 확인
        if (!room.getParticipants().contains(userId)) {
            throw new IllegalArgumentException("User is not a participant of this chat room");
        }
        
        // 메시지 타입 검증
        validateMessageContent(messageDto);
        
        // 데이터베이스에 메시지 저장
        ChatMessage chatMessage = ChatMessage.builder()
                .roomId(messageDto.getRoomId())
                .senderId(userId)
                .type(messageDto.getType())
                .content(messageDto.getContent())
                .s3Key(messageDto.getS3Key())
                .originalMessageId(messageDto.getOriginalMessageId())
                .build();
        
        ChatMessage savedMessage = chatMessageRepository.save(chatMessage);
        
        // STOMP로 메시지 브로드캐스트
        ChatMessageDto savedDto = convertToDto(savedMessage);
        messagingTemplate.convertAndSend("/topic/chat." + messageDto.getRoomId(), savedDto);
        
        return savedDto;
    }

    public List<ChatMessageDto> getRoomMessages(String roomId, Instant before, Integer size) {
        // 권한 확인
        String userId = UserContext.getUserId();
        ChatRoom room = chatRoomService.getRoom(roomId);
        
        if (!room.getParticipants().contains(userId)) {
            throw new IllegalArgumentException("User is not a participant of this chat room");
        }
        
        // 메시지 조회 페이지 크기 설정
        int pageSize = size != null ? Math.min(size, maxPageSize) : defaultPageSize;
        PageRequest pageRequest = PageRequest.of(0, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        
        List<ChatMessage> messages;
        if (before != null) {
            messages = chatMessageRepository.findByRoomIdAndCreatedAtLessThanOrderByCreatedAtDesc(roomId, before, pageRequest);
        } else {
            messages = chatMessageRepository.findByRoomIdOrderByCreatedAtDesc(roomId, pageRequest);
        }
        
        return messages.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public void deleteMessage(String messageId) {
        String userId = UserContext.getUserId();
        
        ChatMessage message = chatMessageRepository.findById(messageId)
                .orElseThrow(() -> new ResourceNotFoundException("Message not found"));
        
        // 메시지 소유자인지 확인
        if (!message.getSenderId().equals(userId)) {
            throw new IllegalArgumentException("User is not the sender of this message");
        }
        
        message.setDeleted(true);
        message.setDeletedAt(Instant.now());
        chatMessageRepository.save(message);
        
        // 삭제된 메시지를 브로드캐스트
        ChatMessageDto deletedDto = convertToDto(message);
        messagingTemplate.convertAndSend("/topic/chat." + message.getRoomId(), deletedDto);
    }

    public ChatMessageDto convertToDto(ChatMessage message) {
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

    private void validateMessageContent(ChatMessageDto messageDto) {
        if (messageDto.getType() == null) {
            throw new IllegalArgumentException("Message type cannot be null");
        }
        
        switch (messageDto.getType()) {
            case TEXT:
                if (messageDto.getContent() == null || messageDto.getContent().isBlank()) {
                    throw new IllegalArgumentException("Text message must have content");
                }
                if (messageDto.getContent().length() > maxTextLength) {
                    throw new IllegalArgumentException("Text message too long, maximum " + maxTextLength + " characters");
                }
                break;
                
            case IMAGE:
                if (messageDto.getS3Key() == null || messageDto.getS3Key().isBlank()) {
                    throw new IllegalArgumentException("Image message must have S3 key");
                }
                break;
                
            case CORRECTION:
                if (messageDto.getOriginalMessageId() == null || messageDto.getOriginalMessageId().isBlank()) {
                    throw new IllegalArgumentException("Correction message must have original message ID");
                }
                if (messageDto.getContent() == null || messageDto.getContent().isBlank()) {
                    throw new IllegalArgumentException("Correction message must have corrected text");
                }
                if (messageDto.getContent().length() > maxTextLength) {
                    throw new IllegalArgumentException("Corrected text too long, maximum " + maxTextLength + " characters");
                }
                
                // 원본 메시지가 존재하는지 확인
                chatMessageRepository.findById(messageDto.getOriginalMessageId())
                        .orElseThrow(() -> new ResourceNotFoundException("Original message not found"));
                break;
                
            default:
                throw new IllegalArgumentException("Unsupported message type: " + messageDto.getType());
        }
    }
} 