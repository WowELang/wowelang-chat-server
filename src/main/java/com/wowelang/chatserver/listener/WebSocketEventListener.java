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
        // SessionConnectEvent 시점에는 아직 인터셉터에서 setUser()가 완료되지 않았을 수 있어 getUser()가 null일 수 있습니다.
        String userIdFromPrincipal = headerAccessor.getUser() != null ? headerAccessor.getUser().getName() : "N/A (Principal)";
        String userIdFromNativeHeader = headerAccessor.getFirstNativeHeader("X-User-Id"); // CONNECT 프레임의 네이티브 헤더 직접 확인
        
        log.debug("웹소켓 연결 요청 (SessionConnectEvent): 세션 ID={}, 사용자 ID (Principal)={}, 사용자 ID (X-User-Id Header)={}", 
                  sessionId, userIdFromPrincipal, userIdFromNativeHeader);
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

        // 추가된 로그: 연결 성공 시 userId 값 확인
        log.info("웹소켓 연결 성공 (SessionConnectedEvent): 세션 ID={}, 사용자 ID={}", sessionId, userId);

        if (userId != null && sessionId != null) {
            log.debug("정상적으로 사용자 ID ({}) 와 세션 ID ({})를 확보하여 세션 등록 및 중복 연결 처리를 시작합니다.", userId, sessionId);
            userSessionRegistry.registerSession(userId, sessionId);
            
            int sessionCount = userSessionRegistry.getUserSessionCount(userId);
            if (sessionCount > 1) {
                int disconnectedCount = webSocketService.disconnectPreviousUserSessions(userId, sessionId);
                log.info("사용자의 이전 연결 제거: 사용자 ID={}, 새 세션 ID={}, 종료된 세션 수={}", 
                        userId, sessionId, disconnectedCount);
            }
        } else {
            // 추가된 로그: userId 또는 sessionId가 null일 경우 경고
            log.warn("웹소켓 연결 성공 후 사용자 ID 또는 세션 ID가 null입니다. 세션 등록 실패. 사용자 ID={}, 세션 ID={}", userId, sessionId);
            // 이 경우, 클라이언트가 X-User-Id를 보내지 않았거나 인터셉터에서 처리가 안됐을 가능성이 높습니다.
            // 또는, 어떤 이유로든 연결은 되었으나 사용자 정보가 누락된 상황입니다.
            // 이런 경우 클라이언트가 스스로 연결을 끊거나, 서버의 다른 로직에 의해 연결이 종료될 수 있습니다.
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