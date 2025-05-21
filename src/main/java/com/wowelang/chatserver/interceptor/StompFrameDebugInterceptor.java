package com.wowelang.chatserver.interceptor;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * STOMP 프레임 디버깅을 위한 인터셉터
 */
@Component
@Slf4j
public class StompFrameDebugInterceptor implements ChannelInterceptor {

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        log.debug("STOMP 프레임 수신: {}", message);
        return message;
    }

    @Override
    public void postSend(Message<?> message, MessageChannel channel, boolean sent) {
        log.debug("STOMP 프레임 전송 완료: {}, 성공: {}", message, sent);
    }

    @Override
    public void afterSendCompletion(Message<?> message, MessageChannel channel, boolean sent, Exception ex) {
        if (ex != null) {
            log.error("STOMP 프레임 전송 중 오류 발생: {}", message, ex);
        }
    }
} 