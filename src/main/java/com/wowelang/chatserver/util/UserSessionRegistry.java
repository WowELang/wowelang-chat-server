package com.wowelang.chatserver.util;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * 사용자 ID별 WebSocket 세션을 관리하는 레지스트리
 * 같은 사용자가 여러 세션에 연결되는 경우 이를 추적하고 관리합니다.
 */
@Slf4j
@Component
public class UserSessionRegistry {
    
    // 사용자 ID를 키로 하여 해당 사용자의 세션 ID 집합을 저장
    private final Map<String, Set<String>> userSessionMap = new ConcurrentHashMap<>();
    
    // 세션 ID를 키로 하여 해당 세션의 사용자 ID를 저장
    private final Map<String, String> sessionUserMap = new ConcurrentHashMap<>();
    
    /**
     * 새로운 사용자 세션을 등록합니다.
     * 
     * @param userId 사용자 ID
     * @param sessionId 세션 ID
     */
    public void registerSession(String userId, String sessionId) {
        // 사용자 ID로 세션 ID 집합 조회 (없으면 새로 생성)
        userSessionMap.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet()).add(sessionId);
        sessionUserMap.put(sessionId, userId);
        log.debug("세션 등록: 사용자 ID={}, 세션 ID={}, 활성 세션 수={}", 
                 userId, sessionId, userSessionMap.get(userId).size());
    }
    
    /**
     * 세션이 종료되면 레지스트리에서 제거합니다.
     * 
     * @param sessionId 세션 ID
     * @return 세션이 제거된 사용자 ID (세션이 존재하지 않는 경우 null)
     */
    public String removeSession(String sessionId) {
        String userId = sessionUserMap.remove(sessionId);
        
        if (userId != null) {
            Set<String> userSessions = userSessionMap.get(userId);
            if (userSessions != null) {
                userSessions.remove(sessionId);
                
                // 사용자의 세션이 모두 종료된 경우 맵에서 사용자 제거
                if (userSessions.isEmpty()) {
                    userSessionMap.remove(userId);
                }
                
                log.debug("세션 제거: 사용자 ID={}, 세션 ID={}, 남은 세션 수={}", 
                         userId, sessionId, userSessions.size());
            }
        }
        
        return userId;
    }
    
    /**
     * 특정 사용자의 모든 활성 세션 ID를 반환합니다.
     * 
     * @param userId 사용자 ID
     * @return 사용자의 활성 세션 ID 집합 (없으면 빈 집합)
     */
    public Set<String> getUserSessions(String userId) {
        return userSessionMap.getOrDefault(userId, Collections.emptySet());
    }
    
    /**
     * 특정 세션의 사용자 ID를 반환합니다.
     * 
     * @param sessionId 세션 ID
     * @return 세션의 사용자 ID (없으면 null)
     */
    public String getUserIdBySessionId(String sessionId) {
        return sessionUserMap.get(sessionId);
    }
    
    /**
     * 특정 사용자의 활성 세션 수를 반환합니다.
     * 
     * @param userId 사용자 ID
     * @return 활성 세션 수
     */
    public int getUserSessionCount(String userId) {
        Set<String> sessions = userSessionMap.get(userId);
        return sessions != null ? sessions.size() : 0;
    }
    
    /**
     * 사용자의 이전 세션을 제거하고 새 세션만 남깁니다.
     * 
     * @param userId 사용자 ID
     * @param newSessionId 유지할 새 세션 ID
     * @return 제거된 세션 ID 집합
     */
    public Set<String> disconnectPreviousSessions(String userId, String newSessionId) {
        Set<String> sessions = userSessionMap.get(userId);
        if (sessions != null && sessions.size() > 1) {
            Set<String> sessionsToRemove = ConcurrentHashMap.newKeySet();
            
            for (String sessionId : sessions) {
                if (!sessionId.equals(newSessionId)) {
                    sessionsToRemove.add(sessionId);
                }
            }
            
            // 세션 맵에서 이전 세션들 제거
            for (String sessionId : sessionsToRemove) {
                sessions.remove(sessionId);
                sessionUserMap.remove(sessionId);
            }
            
            log.debug("이전 세션 제거: 사용자 ID={}, 새 세션 ID={}, 제거된 세션 수={}", 
                     userId, newSessionId, sessionsToRemove.size());
            
            return sessionsToRemove;
        }
        
        return Collections.emptySet();
    }
    
    /**
     * 현재 등록된 모든 사용자 ID를 반환합니다.
     * 
     * @return 활성 사용자 ID 집합
     */
    public Set<String> getAllActiveUsers() {
        return userSessionMap.keySet();
    }
} 