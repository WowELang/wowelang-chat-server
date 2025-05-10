package com.wowelang.chatserver.service;

import java.util.Set;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.wowelang.chatserver.util.UserSessionRegistry;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 웹소켓 연결 관리 서비스
 * 사용자 세션을 관리하고 필요한 경우 강제 연결 종료 메시지를 전송합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WebSocketService {

    private final UserSessionRegistry userSessionRegistry;
    private final SimpMessagingTemplate messagingTemplate;
    
    /**
     * 새로운 연결이 있을 때 동일 사용자의 이전 연결을 종료시키고 알림을 보냅니다.
     * 
     * @param userId 사용자 ID
     * @param newSessionId 새로운 세션 ID
     * @return 종료된 세션 ID 개수
     */
    public int disconnectPreviousUserSessions(String userId, String newSessionId) {
        Set<String> disconnectedSessions = userSessionRegistry.disconnectPreviousSessions(userId, newSessionId);
        
        if (!disconnectedSessions.isEmpty()) {
            // 새 세션에 알림 메시지 전송 (선택 사항)
            messagingTemplate.convertAndSendToUser(
                    userId, 
                    "/queue/notifications", 
                    "다른 디바이스의 동일 계정 접속이 종료되었습니다."
            );
            
            log.info("사용자의 이전 세션 {} 개가 종료되었습니다. 사용자 ID: {}", 
                    disconnectedSessions.size(), userId);
        }
        
        return disconnectedSessions.size();
    }
    
    /**
     * 특정 사용자의 모든 웹소켓 연결 세션 수를 반환합니다.
     * 
     * @param userId 사용자 ID
     * @return 활성 세션 수
     */
    public int getUserActiveSessionCount(String userId) {
        return userSessionRegistry.getUserSessionCount(userId);
    }
    
    /**
     * 특정 사용자가 현재 접속 중인지 확인합니다.
     * 
     * @param userId 사용자 ID
     * @return 접속 여부
     */
    public boolean isUserConnected(String userId) {
        return userSessionRegistry.getUserSessionCount(userId) > 0;
    }
    
    /**
     * 특정 사용자에게 시스템 알림 메시지를 전송합니다.
     * 
     * @param userId 사용자 ID
     * @param message 알림 메시지
     */
    public void sendSystemNotification(String userId, String message) {
        if (isUserConnected(userId)) {
            messagingTemplate.convertAndSendToUser(
                    userId, 
                    "/queue/notifications", 
                    message
            );
            
            log.debug("사용자 {}에게 시스템 알림 전송: {}", userId, message);
        }
    }
    
    /**
     * 현재 접속 중인 모든 사용자 ID 목록을 반환합니다.
     * 
     * @return 활성 사용자 ID 집합
     */
    public Set<String> getAllConnectedUsers() {
        return userSessionRegistry.getAllActiveUsers();
    }
} 