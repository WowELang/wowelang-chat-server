package com.wowelang.chatserver.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.support.WebSocketHandlerMapping;

/**
 * WebSocket 핸들러 매핑 우선순위를 설정하는 클래스
 * 정적 리소스 핸들러보다 WebSocket 핸들러가 먼저 처리되도록 우선순위를 높게 설정합니다.
 */
@Configuration
public class WebSocketHandlerConfig {

    /**
     * WebSocketHandlerMapping의 우선순위를 최상위로 설정합니다.
     * 이는 SimpleUrlHandlerMapping보다 높은 우선순위를 가지게 합니다.
     */
    @Bean
    public HandlerMapping webSocketHandlerMapping() {
        WebSocketHandlerMapping mapping = new WebSocketHandlerMapping();
        mapping.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return mapping;
    }
} 