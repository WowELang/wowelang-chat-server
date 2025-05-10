package com.wowelang.chatserver.listener;

import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import com.wowelang.chatserver.service.WebSocketService;
import com.wowelang.chatserver.util.UserSessionRegistry;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * WebSocket 이벤트(연결, 연결 해제 등)를 처리하는 리스너
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketEventListener {

    private final UserSessionRegistry userSessionRegistry;
    private final WebSocketService webSocketService;

    /**
     * 웹소켓 연결 요청 이벤트를 처리합니다.
     * 이벤트는 클라이언트가 연결을 시도하면 발생합니다.
     */
    @EventListener
    public void handleWebSocketConnectListener(SessionConnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        String userId = headerAccessor.getUser() != null ? headerAccessor.getUser().getName() : null;

        log.debug("웹소켓 연결 요청: 세션 ID={}, 사용자 ID={}", sessionId, userId);
    }

    /**
     * 웹소켓 연결 성공 이벤트를 처리합니다.
     * 이 이벤트는 연결이 성공적으로 수립된 후 발생합니다.
     */
    @EventListener
    public void handleWebSocketConnectedListener(SessionConnectedEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        String userId = headerAccessor.getUser() != null ? headerAccessor.getUser().getName() : null;

        if (userId != null && sessionId != null) {
            // 사용자 세션 등록
            userSessionRegistry.registerSession(userId, sessionId);
            
            // 중복 연결 확인 및 이전 세션 해제
            int sessionCount = userSessionRegistry.getUserSessionCount(userId);
            if (sessionCount > 1) {
                // 웹소켓 서비스를 통해 이전 세션들 종료 처리
                int disconnectedCount = webSocketService.disconnectPreviousUserSessions(userId, sessionId);
                
                log.info("사용자의 이전 연결 제거: 사용자 ID={}, 새 세션 ID={}, 종료된 세션 수={}", 
                        userId, sessionId, disconnectedCount);
            }
        }
    }

    /**
     * 웹소켓 연결 해제 이벤트를 처리합니다.
     * 이 이벤트는 클라이언트 연결이 종료될 때 발생합니다.
     */
    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        
        // 세션 레지스트리에서 해당 세션을 제거하고 연결된 사용자 ID 반환
        String userId = userSessionRegistry.removeSession(sessionId);
        
        if (userId != null) {
            log.info("사용자 웹소켓 연결 종료: 사용자 ID={}, 세션 ID={}", userId, sessionId);
            
            // 필요한 경우 추가 처리
        }
    }
} 