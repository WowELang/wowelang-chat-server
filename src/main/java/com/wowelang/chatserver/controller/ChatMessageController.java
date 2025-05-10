package com.wowelang.chatserver.controller;

import java.time.Instant;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.wowelang.chatserver.dto.ChatMessageDto;
import com.wowelang.chatserver.service.ChatMessageService;
import com.wowelang.chatserver.util.UserContext;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/rooms/{roomId}/messages")
@RequiredArgsConstructor
public class ChatMessageController {

    private final ChatMessageService chatMessageService;

    @MessageMapping("/chat.send.{roomId}")
    public void sendMessage(@DestinationVariable String roomId, @Valid ChatMessageDto messageDto, StompHeaderAccessor accessor) {
        // 헤더에서 직접 사용자 ID 가져오기
        String userId = accessor.getUser() != null ? accessor.getUser().getName() : null;
        
        // UserContext에 사용자 ID 설정
        if (userId != null) {
            UserContext.setUserId(userId);
        }
        
        messageDto.setRoomId(roomId);
        messageDto.setSenderId(userId); // 메시지에 발신자 ID 명시적으로 설정
        
        chatMessageService.sendMessage(messageDto);
    }

    @GetMapping
    public ResponseEntity<List<ChatMessageDto>> getMessages(
            @PathVariable String roomId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant before,
            @RequestParam(required = false) Integer size) {
        
        return ResponseEntity.ok(chatMessageService.getRoomMessages(roomId, before, size));
    }

    @DeleteMapping("/{messageId}")
    public ResponseEntity<Void> deleteMessage(@PathVariable String messageId) {
        chatMessageService.deleteMessage(messageId);
        return ResponseEntity.noContent().build();
    }
} 