package com.wowelang.chatserver.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import com.wowelang.chatserver.exception.ResourceNotFoundException;
import com.wowelang.chatserver.model.ChatRoom;
import com.wowelang.chatserver.repository.ChatMessageRepository;
import com.wowelang.chatserver.repository.ChatRoomRepository;
import com.wowelang.chatserver.util.UserContext;

@ExtendWith(MockitoExtension.class)
class ChatRoomServiceTest {

    @Mock
    private ChatRoomRepository chatRoomRepository;
    
    @Mock
    private ChatMessageRepository chatMessageRepository;
    
    @Mock
    private ChatMessageService chatMessageService;
    
    @InjectMocks
    private ChatRoomService chatRoomService;
    
    private static final String USER_ID = "user1";
    private static final String OTHER_USER_ID = "user2";
    private static final String ROOM_ID = "room1";
    
    @BeforeEach
    void setUp() {
        // Mock static method
        try (MockedStatic<UserContext> userContextMock = mockStatic(UserContext.class)) {
            userContextMock.when(UserContext::getUserId).thenReturn(USER_ID);
        }
    }
    
    @Test
    void testCreateRoom_success() {
        // Given
        List<String> participants = Arrays.asList(USER_ID, OTHER_USER_ID);
        ChatRoom expectedRoom = new ChatRoom();
        expectedRoom.setId(ROOM_ID);
        expectedRoom.setParticipants(participants);
        
        when(chatRoomRepository.findByParticipants(USER_ID, OTHER_USER_ID))
            .thenReturn(Optional.empty());
        when(chatRoomRepository.save(any(ChatRoom.class)))
            .thenReturn(expectedRoom);
        
        // When
        ChatRoom result = chatRoomService.createRoom(participants);
        
        // Then
        assertNotNull(result);
        assertEquals(ROOM_ID, result.getId());
        assertEquals(participants, result.getParticipants());
        verify(chatRoomRepository).findByParticipants(USER_ID, OTHER_USER_ID);
        verify(chatRoomRepository).save(any(ChatRoom.class));
    }
    
    @Test
    void testCreateRoom_existingRoom() {
        // Given
        List<String> participants = Arrays.asList(USER_ID, OTHER_USER_ID);
        ChatRoom existingRoom = new ChatRoom();
        existingRoom.setId(ROOM_ID);
        existingRoom.setParticipants(participants);
        
        when(chatRoomRepository.findByParticipants(USER_ID, OTHER_USER_ID))
            .thenReturn(Optional.of(existingRoom));
        
        // When
        ChatRoom result = chatRoomService.createRoom(participants);
        
        // Then
        assertNotNull(result);
        assertEquals(ROOM_ID, result.getId());
        assertEquals(participants, result.getParticipants());
        verify(chatRoomRepository).findByParticipants(USER_ID, OTHER_USER_ID);
        verify(chatRoomRepository, never()).save(any(ChatRoom.class));
    }
    
    @Test
    void testCreateRoom_invalidParticipants() {
        // Given
        List<String> participants = Arrays.asList(USER_ID);
        
        // When & Then
        Exception exception = assertThrows(IllegalArgumentException.class, 
                () -> chatRoomService.createRoom(participants));
        
        assertEquals("Chat room must have exactly 2 participants", exception.getMessage());
    }
    
    @Test
    void testGetRoom_success() {
        // Given
        ChatRoom room = new ChatRoom();
        room.setId(ROOM_ID);
        room.setDeleted(false);
        
        when(chatRoomRepository.findById(ROOM_ID))
            .thenReturn(Optional.of(room));
        
        // When
        ChatRoom result = chatRoomService.getRoom(ROOM_ID);
        
        // Then
        assertNotNull(result);
        assertEquals(ROOM_ID, result.getId());
        assertFalse(result.isDeleted());
    }
    
    @Test
    void testGetRoom_notFound() {
        // Given
        when(chatRoomRepository.findById(ROOM_ID))
            .thenReturn(Optional.empty());
        
        // When & Then
        Exception exception = assertThrows(ResourceNotFoundException.class, 
                () -> chatRoomService.getRoom(ROOM_ID));
        
        assertEquals("Chat room not found", exception.getMessage());
    }
    
    @Test
    void testGetRoom_deletedRoom() {
        // Given
        ChatRoom room = new ChatRoom();
        room.setId(ROOM_ID);
        room.setDeleted(true);
        
        when(chatRoomRepository.findById(ROOM_ID))
            .thenReturn(Optional.of(room));
        
        // When & Then
        Exception exception = assertThrows(ResourceNotFoundException.class, 
                () -> chatRoomService.getRoom(ROOM_ID));
        
        assertEquals("Chat room not found", exception.getMessage());
    }
    
    @Test
    void testDeleteRoom_success() {
        // Given
        List<String> participants = Arrays.asList(USER_ID, OTHER_USER_ID);
        ChatRoom room = new ChatRoom();
        room.setId(ROOM_ID);
        room.setParticipants(participants);
        room.setDeleted(false);
        
        when(chatRoomRepository.findById(ROOM_ID))
            .thenReturn(Optional.of(room));
        
        try (MockedStatic<UserContext> userContextMock = mockStatic(UserContext.class)) {
            userContextMock.when(UserContext::getUserId).thenReturn(USER_ID);
            
            // When
            chatRoomService.deleteRoom(ROOM_ID);
            
            // Then
            verify(chatRoomRepository).save(argThat(savedRoom -> 
                savedRoom.isDeleted() && savedRoom.getDeletedAt() != null));
        }
    }
    
    @Test
    void testDeleteRoom_notParticipant() {
        // Given
        List<String> participants = Arrays.asList("otherUser", OTHER_USER_ID);
        ChatRoom room = new ChatRoom();
        room.setId(ROOM_ID);
        room.setParticipants(participants);
        room.setDeleted(false);
        
        when(chatRoomRepository.findById(ROOM_ID))
            .thenReturn(Optional.of(room));
        
        try (MockedStatic<UserContext> userContextMock = mockStatic(UserContext.class)) {
            userContextMock.when(UserContext::getUserId).thenReturn(USER_ID);
            
            // When & Then
            Exception exception = assertThrows(IllegalArgumentException.class, 
                    () -> chatRoomService.deleteRoom(ROOM_ID));
            
            assertEquals("User is not a participant of this room", exception.getMessage());
        }
    }
} 