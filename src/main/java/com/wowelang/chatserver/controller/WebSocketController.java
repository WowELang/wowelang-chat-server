package com.wowelang.chatserver.controller;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 웹소켓 연결 상태를 관리하는 컨트롤러
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class WebSocketController {

    /**
     * 웹소켓 연결 상태 확인용 핑-퐁 메시지 처리
     * 
     * @param message 클라이언트에서 보낸 메시지
     * @return 서버에서 응답하는 메시지
     */
    @MessageMapping("/ping")
    @SendTo("/topic/pong")
    public String handlePing(String message) {
        log.debug("Received ping message: {}", message);
        return "pong: " + message;
    }
} 