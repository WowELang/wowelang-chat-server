package com.wowelang.chatserver.integration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.Transport;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

import com.wowelang.chatserver.dto.ChatMessageDto;
import com.wowelang.chatserver.model.ChatMessage.MessageType;
import com.wowelang.chatserver.model.ChatRoom;
import com.wowelang.chatserver.repository.ChatRoomRepository;
import com.wowelang.chatserver.service.ChatRoomService;
import com.wowelang.chatserver.util.UserContext;

import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class ChatServerIntegrationTest {

    @LocalServerPort
    private int port;
    
    @Autowired
    private ChatRoomService chatRoomService;
    
    @Autowired
    private ChatRoomRepository chatRoomRepository;
    
    @MockBean
    private S3Client s3Client;
    
    @MockBean
    private S3Presigner s3Presigner;
    
    private static final String USER_1 = "user1";
    private static final String USER_2 = "user2";
    
    private WebSocketStompClient stompClient;
    private StompSession stompSession;
    private String chatRoomId;
    
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", () -> "mongodb://localhost:27017/chattest");
    }
    
    @BeforeEach
    void setup() throws Exception {
        // 웹소켓 클라이언트 설정
        List<Transport> transports = List.of(new WebSocketTransport(new StandardWebSocketClient()));
        SockJsClient sockJsClient = new SockJsClient(transports);
        
        stompClient = new WebSocketStompClient(sockJsClient);
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());
        
        // 채팅방 생성
        try (MockedStatic<UserContext> userContextMock = mockStatic(UserContext.class)) {
            userContextMock.when(UserContext::getUserId).thenReturn(USER_1);
            
            ChatRoom room = chatRoomService.createRoom(Arrays.asList(USER_1, USER_2));
            chatRoomId = room.getId();
        }
        
        // 웹소켓 연결
        StompHeaders headers = new StompHeaders();
        headers.add("X-User-Id", USER_1);
        
        CompletableFuture<StompSession> sessionFuture = stompClient.connectAsync(
                "ws://localhost:" + port + "/ws", 
                new StompSessionHandlerAdapter() {}, 
                headers);
        
        stompSession = sessionFuture.get(5, TimeUnit.SECONDS);
    }
    
    @AfterEach
    void cleanup() {
        if (stompSession != null && stompSession.isConnected()) {
            stompSession.disconnect();
        }
        
        chatRoomRepository.deleteAll();
    }
    
    @Test
    void testWebSocketChatMessage() throws Exception {
        // 메시지 수신을 위한 블로킹 큐 설정
        BlockingQueue<ChatMessageDto> receivedMessages = new LinkedBlockingQueue<>();
        
        // 토픽 구독
        stompSession.subscribe("/topic/chat." + chatRoomId, new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return ChatMessageDto.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                receivedMessages.add((ChatMessageDto) payload);
            }
        });
        
        // 메시지 전송
        ChatMessageDto message = new ChatMessageDto();
        message.setRoomId(chatRoomId);
        message.setType(MessageType.TEXT);
        message.setContent("Hello from integration test!");
        
        stompSession.send("/app/chat.send." + chatRoomId, message);
        
        // 메시지 수신 확인
        ChatMessageDto receivedMessage = receivedMessages.poll(5, TimeUnit.SECONDS);
        assertNotNull(receivedMessage);
        assertEquals(chatRoomId, receivedMessage.getRoomId());
        assertEquals(USER_1, receivedMessage.getSenderId());
        assertEquals(MessageType.TEXT, receivedMessage.getType());
        assertEquals("Hello from integration test!", receivedMessage.getContent());
    }
} 