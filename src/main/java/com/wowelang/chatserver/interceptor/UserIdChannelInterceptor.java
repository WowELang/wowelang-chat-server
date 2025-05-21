package com.wowelang.chatserver.interceptor;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import com.wowelang.chatserver.util.UserContext;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * WebSocket 채널 인터셉터
 * 메시지가 전송되기 전에 사용자 ID를 처리합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserIdChannelInterceptor implements ChannelInterceptor {

    private static final String USER_ID_HEADER = "X-User-Id";

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        
        if (accessor != null) {
            // 연결 시 사용자 ID 설정
            if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String userId = accessor.getFirstNativeHeader(USER_ID_HEADER);
            if (userId != null) {
                accessor.setUser(() -> userId);
                    log.debug("사용자 연결: ID={}", userId);
            } else {
                    log.warn("사용자 ID 없이 연결 시도");
                }
            } 
            // 메시지 전송 시 사용자 컨텍스트 설정
            else if (StompCommand.SEND.equals(accessor.getCommand()) || StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
                if (accessor.getUser() != null) {
                    String userId = accessor.getUser().getName();
                    UserContext.setUserId(userId);
                    log.debug("메시지 전송 시 사용자 ID 설정: {}", userId);
                }
            }
            // 연결 해제 시 사용자 컨텍스트 정리
            else if (StompCommand.DISCONNECT.equals(accessor.getCommand())) {
                if (accessor.getUser() != null) {
                    String userId = accessor.getUser().getName();
                    log.debug("사용자 연결 해제: ID={}", userId);
                    UserContext.clear();
                }
            }
        }
        
        return message;
    }
    
    @Override
    public void afterSendCompletion(Message<?> message, MessageChannel channel, boolean sent, Exception ex) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        
        // 메시지 전송 후 ThreadLocal 정리
        if (accessor != null && 
            (StompCommand.SEND.equals(accessor.getCommand()) || 
             StompCommand.SUBSCRIBE.equals(accessor.getCommand()))) {
            UserContext.clear();
        }
    }
} 