package com.wowelang.chatserver.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/**
 * 로우 레벨의 WebSocket 프레임을 디버깅하기 위한 커스텀 핸들러 설정
 */
@Slf4j
@Configuration
public class CustomStompWebSocketHandlerConfig implements WebSocketConfigurer {

    @Value("${websocket.endpoint}")
    private String endpoint;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // 디버그 엔드포인트를 별도로 등록
        registry.addHandler(rawWebSocketHandler(), endpoint + "-debug")
                .setAllowedOriginPatterns("*");
    }

    @Bean
    public WebSocketHandler rawWebSocketHandler() {
        return new RawWebSocketHandler();
    }

    /**
     * 원시 웹소켓 메시지를 로깅하는 핸들러
     */
    private static class RawWebSocketHandler extends TextWebSocketHandler {
        
        @Override
        public void afterConnectionEstablished(WebSocketSession session) {
            log.debug("RawWebSocketHandler - 새 웹소켓 연결 수립: {}", session.getId());
        }

        @Override
        protected void handleTextMessage(WebSocketSession session, TextMessage message) {
            byte[] payload = message.getPayload().getBytes();
            
            log.debug("RawWebSocketHandler - 수신된 메시지, 세션: {}", session.getId());
            log.debug("메시지 페이로드 크기: {} 바이트", payload.length);
            
            // 16진수로 출력
            StringBuilder hexString = new StringBuilder();
            for (byte b : payload) {
                hexString.append(String.format("%02X ", b));
            }
            log.debug("바이너리 데이터(16진수): {}", hexString.toString());
            
            // 문자열로 출력
            log.debug("문자열 데이터: '{}'", message.getPayload());
            
            // STOMP 프레임 유효성 검사
            String text = message.getPayload();
            if (text.startsWith("CONNECT")) {
                boolean hasDoubleNewline = text.contains("\n\n");
                boolean endsWithNull = text.endsWith("\u0000");
                
                log.debug("STOMP CONNECT 프레임 검사:");
                log.debug("이중 개행 문자 존재 (헤더-본문 구분): {}", hasDoubleNewline);
                log.debug("NULL 종결자 존재: {}", endsWithNull);
                
                if (!hasDoubleNewline || !endsWithNull) {
                    log.warn("유효하지 않은 STOMP 프레임 형식 감지됨!");
                }
            }
        }

        @Override
        public void handleTransportError(WebSocketSession session, Throwable exception) {
            log.error("RawWebSocketHandler - 전송 오류 발생, 세션: {}", session.getId(), exception);
        }

        @Override
        public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
            log.debug("RawWebSocketHandler - 웹소켓 연결 종료, 세션: {}, 상태: {}", session.getId(), status);
        }
    }
} 