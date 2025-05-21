package com.wowelang.chatserver.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.handler.WebSocketHandlerDecorator;
import org.springframework.web.socket.handler.WebSocketHandlerDecoratorFactory;

import com.wowelang.chatserver.interceptor.StompFrameDebugInterceptor;
import com.wowelang.chatserver.interceptor.UserIdChannelInterceptor;
import com.wowelang.chatserver.decorator.EmaWebSocketHandlerDecorator;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Value("${websocket.endpoint}")
    private String endpoint;

    // allowedOrigins 변수는 현재 setAllowedOriginPatterns("*")로 대체되었으므로 주석 처리하거나 삭제 가능
    // @Value("${websocket.allowed-origins}")
    // private String allowedOrigins;

    private final UserIdChannelInterceptor userIdChannelInterceptor;
    private final StompFrameDebugInterceptor stompFrameDebugInterceptor;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic", "/queue");
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint(endpoint)
                .setAllowedOriginPatterns("*");  // .withSockJS() 제거
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        // StompFrameDebugInterceptor를 먼저 등록하여 원시 프레임을 먼저 로깅
        registration.interceptors(stompFrameDebugInterceptor, userIdChannelInterceptor);
    }
    
    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration registration) {
        registration.addDecoratorFactory(new WebSocketHandlerDecoratorFactory() {
            @Override
            public WebSocketHandler decorate(WebSocketHandler webSocketHandler) {
                return new EmaWebSocketHandlerDecorator(webSocketHandler);
            }
        });
    }
} 