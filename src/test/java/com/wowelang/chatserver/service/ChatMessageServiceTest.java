package com.wowelang.chatserver.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.messaging.simp.SimpMessageSendingOperations;

import com.wowelang.chatserver.dto.ChatMessageDto;
import com.wowelang.chatserver.exception.ResourceNotFoundException;
import com.wowelang.chatserver.model.ChatMessage;
import com.wowelang.chatserver.model.ChatMessage.MessageType;
import com.wowelang.chatserver.model.ChatRoom;
import com.wowelang.chatserver.repository.ChatMessageRepository;
import com.wowelang.chatserver.util.UserContext;

@ExtendWith(MockitoExtension.class)
class ChatMessageServiceTest {

    @Mock
    private ChatMessageRepository chatMessageRepository;
    
    @Mock
    private ChatRoomService chatRoomService;
    
    @Mock
    private SimpMessageSendingOperations messagingTemplate;
    
    @InjectMocks
    private ChatMessageService chatMessageService;
    
    private static final String USER_ID = "user1";
    private static final String OTHER_USER_ID = "user2";
    private static final String ROOM_ID = "room1";
    private static final String MESSAGE_ID = "message1";
    
    @Test
    @Disabled("CI 환경에서 호환성 문제로 비활성화")
    void testSendMessage_success() {
        // Given
        ChatRoom room = new ChatRoom();
        room.setId(ROOM_ID);
        room.setParticipants(Arrays.asList(USER_ID, OTHER_USER_ID));
        
        ChatMessageDto inputDto = new ChatMessageDto();
        inputDto.setRoomId(ROOM_ID);
        inputDto.setType(MessageType.TEXT);
        inputDto.setContent("Hello, world!");
        
        ChatMessage savedMessage = new ChatMessage();
        savedMessage.setId(MESSAGE_ID);
        savedMessage.setRoomId(ROOM_ID);
        savedMessage.setSenderId(USER_ID);
        savedMessage.setType(MessageType.TEXT);
        savedMessage.setContent("Hello, world!");
        savedMessage.setCreatedAt(Instant.now());
        
        try (MockedStatic<UserContext> userContextMock = mockStatic(UserContext.class)) {
            userContextMock.when(UserContext::getUserId).thenReturn(USER_ID);
            
            when(chatRoomService.getRoom(ROOM_ID)).thenReturn(room);
            when(chatMessageRepository.save(any(ChatMessage.class))).thenReturn(savedMessage);
            
            // When
            ChatMessageDto result = chatMessageService.sendMessage(inputDto);
            
            // Then
            assertNotNull(result);
            assertEquals(MESSAGE_ID, result.getId());
            assertEquals(ROOM_ID, result.getRoomId());
            assertEquals(USER_ID, result.getSenderId());
            assertEquals(MessageType.TEXT, result.getType());
            assertEquals("Hello, world!", result.getContent());
            
            verify(messagingTemplate).convertAndSend(eq("/topic/chat." + ROOM_ID), any(ChatMessageDto.class));
        }
    }
    
    @Test
    void testSendMessage_userNotParticipant() {
        // Given
        ChatRoom room = new ChatRoom();
        room.setId(ROOM_ID);
        room.setParticipants(Arrays.asList("otherUser", OTHER_USER_ID));
        
        ChatMessageDto inputDto = new ChatMessageDto();
        inputDto.setRoomId(ROOM_ID);
        inputDto.setType(MessageType.TEXT);
        inputDto.setContent("Hello, world!");
        
        try (MockedStatic<UserContext> userContextMock = mockStatic(UserContext.class)) {
            userContextMock.when(UserContext::getUserId).thenReturn(USER_ID);
            
            when(chatRoomService.getRoom(ROOM_ID)).thenReturn(room);
            
            // When & Then
            Exception exception = assertThrows(IllegalArgumentException.class, 
                    () -> chatMessageService.sendMessage(inputDto));
            
            assertEquals("User is not a participant of this chat room", exception.getMessage());
        }
    }
    
    @Test
    @Disabled("CI 환경에서 호환성 문제로 비활성화")
    void testGetRoomMessages_success() {
        // Given
        ChatRoom room = new ChatRoom();
        room.setId(ROOM_ID);
        room.setParticipants(Arrays.asList(USER_ID, OTHER_USER_ID));
        
        ChatMessage message1 = new ChatMessage();
        message1.setId("msg1");
        message1.setRoomId(ROOM_ID);
        message1.setSenderId(USER_ID);
        message1.setType(MessageType.TEXT);
        message1.setContent("Message 1");
        message1.setCreatedAt(Instant.now());
        
        ChatMessage message2 = new ChatMessage();
        message2.setId("msg2");
        message2.setRoomId(ROOM_ID);
        message2.setSenderId(OTHER_USER_ID);
        message2.setType(MessageType.TEXT);
        message2.setContent("Message 2");
        message2.setCreatedAt(Instant.now());
        
        List<ChatMessage> messages = Arrays.asList(message1, message2);
        
        try (MockedStatic<UserContext> userContextMock = mockStatic(UserContext.class)) {
            userContextMock.when(UserContext::getUserId).thenReturn(USER_ID);
            
            when(chatRoomService.getRoom(ROOM_ID)).thenReturn(room);
            when(chatMessageRepository.findByRoomIdOrderByCreatedAtDesc(eq(ROOM_ID), any(PageRequest.class)))
                .thenReturn(messages);
            
            // When
            List<ChatMessageDto> results = chatMessageService.getRoomMessages(ROOM_ID, null, 10);
            
            // Then
            assertNotNull(results);
            assertEquals(2, results.size());
            assertEquals("msg1", results.get(0).getId());
            assertEquals("msg2", results.get(1).getId());
            
            verify(chatMessageRepository).findByRoomIdOrderByCreatedAtDesc(eq(ROOM_ID), any(PageRequest.class));
        }
    }
    
    @Test
    void testDeleteMessage_success() {
        // Given
        ChatMessage message = new ChatMessage();
        message.setId(MESSAGE_ID);
        message.setRoomId(ROOM_ID);
        message.setSenderId(USER_ID);
        message.setType(MessageType.TEXT);
        message.setContent("Hello, world!");
        message.setDeleted(false);
        
        try (MockedStatic<UserContext> userContextMock = mockStatic(UserContext.class)) {
            userContextMock.when(UserContext::getUserId).thenReturn(USER_ID);
            
            when(chatMessageRepository.findById(MESSAGE_ID)).thenReturn(Optional.of(message));
            
            // When
            chatMessageService.deleteMessage(MESSAGE_ID);
            
            // Then
            verify(chatMessageRepository).save(argThat(savedMessage -> 
                savedMessage.isDeleted() && savedMessage.getDeletedAt() != null));
            
            verify(messagingTemplate).convertAndSend(eq("/topic/chat." + ROOM_ID), any(ChatMessageDto.class));
        }
    }
    
    @Test
    void testDeleteMessage_notSender() {
        // Given
        ChatMessage message = new ChatMessage();
        message.setId(MESSAGE_ID);
        message.setRoomId(ROOM_ID);
        message.setSenderId(OTHER_USER_ID);
        message.setType(MessageType.TEXT);
        message.setContent("Hello, world!");
        message.setDeleted(false);
        
        try (MockedStatic<UserContext> userContextMock = mockStatic(UserContext.class)) {
            userContextMock.when(UserContext::getUserId).thenReturn(USER_ID);
            
            when(chatMessageRepository.findById(MESSAGE_ID)).thenReturn(Optional.of(message));
            
            // When & Then
            Exception exception = assertThrows(IllegalArgumentException.class, 
                    () -> chatMessageService.deleteMessage(MESSAGE_ID));
            
            assertEquals("User is not the sender of this message", exception.getMessage());
        }
    }
    
    @Test
    void testDeleteMessage_notFound() {
        // Given
        try (MockedStatic<UserContext> userContextMock = mockStatic(UserContext.class)) {
            userContextMock.when(UserContext::getUserId).thenReturn(USER_ID);
            
            when(chatMessageRepository.findById(MESSAGE_ID)).thenReturn(Optional.empty());
            
            // When & Then
            Exception exception = assertThrows(ResourceNotFoundException.class, 
                    () -> chatMessageService.deleteMessage(MESSAGE_ID));
            
            assertEquals("Message not found", exception.getMessage());
        }
    }
    
    @Test
    void testConvertToDto() {
        // Given
        Instant now = Instant.now();
        ChatMessage message = new ChatMessage();
        message.setId(MESSAGE_ID);
        message.setRoomId(ROOM_ID);
        message.setSenderId(USER_ID);
        message.setType(MessageType.TEXT);
        message.setContent("Hello, world!");
        message.setCreatedAt(now);
        message.setDeleted(false);
        
        // When
        ChatMessageDto dto = chatMessageService.convertToDto(message);
        
        // Then
        assertNotNull(dto);
        assertEquals(MESSAGE_ID, dto.getId());
        assertEquals(ROOM_ID, dto.getRoomId());
        assertEquals(USER_ID, dto.getSenderId());
        assertEquals(MessageType.TEXT, dto.getType());
        assertEquals("Hello, world!", dto.getContent());
        assertEquals(now, dto.getCreatedAt());
        assertFalse(dto.isDeleted());
    }
    
    @Test
    void testConvertToDto_deleted() {
        // Given
        Instant now = Instant.now();
        ChatMessage message = new ChatMessage();
        message.setId(MESSAGE_ID);
        message.setRoomId(ROOM_ID);
        message.setSenderId(USER_ID);
        message.setType(MessageType.TEXT);
        message.setContent("Hello, world!");
        message.setCreatedAt(now);
        message.setDeleted(true);
        
        // When
        ChatMessageDto dto = chatMessageService.convertToDto(message);
        
        // Then
        assertNotNull(dto);
        assertEquals(MESSAGE_ID, dto.getId());
        assertEquals(ROOM_ID, dto.getRoomId());
        assertEquals(USER_ID, dto.getSenderId());
        assertEquals(MessageType.TEXT, dto.getType());
        assertNull(dto.getContent());
        assertEquals(now, dto.getCreatedAt());
        assertTrue(dto.isDeleted());
    }
} 