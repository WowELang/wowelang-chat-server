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
        
        // 추가된 로그: preSend 메서드 호출 및 StompCommand 확인
        if (accessor != null) {
            log.debug("UserIdChannelInterceptor preSend 호출: Command={}", accessor.getCommand());

            try {
                // 연결 시 사용자 ID 설정
                if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                    log.debug("STOMP CONNECT 처리 시작. Native Headers: {}", accessor.toNativeHeaderMap());
                    String userId = accessor.getFirstNativeHeader(USER_ID_HEADER);
                    if (userId != null) {
                        accessor.setUser(() -> userId);
                        log.info("사용자 ID '{}'에 대해 STOMP 세션 사용자 설정 완료. SessionId={}", userId, accessor.getSessionId());
                    } else {
                        log.warn("STOMP CONNECT 헤더에 '{}'가 없습니다. SessionId={}", USER_ID_HEADER, accessor.getSessionId());
                        // 사용자 ID가 없으면 연결을 진행시키지 않으려면 여기서 메시지를 null로 반환하거나 예외를 던질 수 있습니다.
                        // 예를 들어: throw new IllegalArgumentException("X-User-Id header is missing");
                        // 하지만 현재는 경고만 로깅하고 연결은 시도합니다.
                    }
                } 
                // 메시지 전송 시 사용자 컨텍스트 설정
                else if (StompCommand.SEND.equals(accessor.getCommand()) || StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
                    if (accessor.getUser() != null) {
                        String userId = accessor.getUser().getName();
                        UserContext.setUserId(userId);
                        log.debug("메시지 전송/구독 시 사용자 ID 설정: {}. SessionId={}", userId, accessor.getSessionId());
                    } else {
                        log.warn("SEND/SUBSCRIBE: accessor.getUser()가 null입니다. UserContext 설정 불가. SessionId={}", accessor.getSessionId());
                    }
                }
                // 연결 해제 시 사용자 컨텍스트 정리
                else if (StompCommand.DISCONNECT.equals(accessor.getCommand())) {
                    if (accessor.getUser() != null) {
                        String userId = accessor.getUser().getName();
                        log.debug("사용자 연결 해제: ID={}. SessionId={}", userId, accessor.getSessionId());
                        UserContext.clear();
                    } else {
                        log.debug("DISCONNECT: accessor.getUser()가 null입니다. SessionId={}", accessor.getSessionId());
                        UserContext.clear(); // 사용자 정보가 없더라도 컨텍스트는 클리어
                    }
                }
            } catch (Exception e) {
                log.error("UserIdChannelInterceptor preSend 처리 중 예외 발생: Command={}, SessionId={}", accessor.getCommand(), accessor.getSessionId(), e);
                // 필요시 여기서 예외를 다시 던지거나, 메시지를 null로 만들어 전송을 막을 수 있습니다.
                // throw e; 
            }
        } else {
            log.warn("UserIdChannelInterceptor preSend 호출: StompHeaderAccessor가 null입니다.");
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
            // 추가된 로그
            if (accessor.getUser() != null) {
                log.debug("afterSendCompletion: UserContext 클리어. UserId={}", accessor.getUser().getName());
            } else {
                log.debug("afterSendCompletion: UserContext 클리어 (사용자 정보 없음).");
            }
        }

        if (ex != null) {
            log.error("UserIdChannelInterceptor afterSendCompletion: 메시지 전송 실패. SessionId={}", accessor != null ? accessor.getSessionId() : "N/A", ex);
        }
    }
}