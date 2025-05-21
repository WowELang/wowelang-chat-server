package com.wowelang.chatserver.interceptor;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * STOMP 프레임 디버깅을 위한 인터셉터
 * 웹소켓으로 전송되는 STOMP 프레임의 바이너리 내용을 로깅합니다.
 */
@Slf4j
@Component
public class StompFrameDebugInterceptor implements ChannelInterceptor {

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        
        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            log.debug("=== STOMP CONNECT 프레임 디버깅 시작 ===");
            log.debug("STOMP Headers: {}", accessor.toNativeHeaderMap());
            
            // 메시지 페이로드 디버깅
            Object payload = message.getPayload();
            if (payload instanceof byte[]) {
                byte[] bytes = (byte[]) payload;
                log.debug("페이로드 크기: {} 바이트", bytes.length);
                
                // 바이너리 데이터 출력
                log.debug("바이너리 데이터(10진수): {}", Arrays.toString(bytes));
                
                // 16진수로 출력
                StringBuilder hexString = new StringBuilder();
                for (byte b : bytes) {
                    hexString.append(String.format("%02X ", b));
                }
                log.debug("바이너리 데이터(16진수): {}", hexString.toString());
                
                // 문자열로 출력
                String text = new String(bytes, StandardCharsets.UTF_8);
                log.debug("문자열 데이터: '{}'", text);
                
                // NULL 문자 확인
                boolean hasNullTerminator = bytes.length > 0 && bytes[bytes.length - 1] == 0;
                log.debug("NULL 종결자 존재: {}", hasNullTerminator);
                
                // 이중 개행 문자 검사 (헤더와 본문 구분)
                int doubleNewlineIndex = text.indexOf("\n\n");
                boolean hasDoubleNewline = doubleNewlineIndex != -1;
                log.debug("이중 개행 문자 존재: {}, 위치: {}", hasDoubleNewline, doubleNewlineIndex);
            }
            log.debug("=== STOMP CONNECT 프레임 디버깅 종료 ===");
        }
        
        return message;
    }
} 