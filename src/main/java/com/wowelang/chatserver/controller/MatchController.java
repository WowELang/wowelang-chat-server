package com.wowelang.chatserver.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.wowelang.chatserver.dto.MatchRequestDto;
import com.wowelang.chatserver.model.MatchRequest.MatchStatus;
import com.wowelang.chatserver.service.MatchRequestService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/matches")
@RequiredArgsConstructor
public class MatchController {

    private final MatchRequestService matchRequestService;
    
    @PostMapping("/{targetId}")
    public ResponseEntity<MatchRequestDto> sendMatchRequest(@PathVariable String targetId) {
        return ResponseEntity.ok(matchRequestService.sendMatchRequest(targetId));
    }
    
    @PostMapping("/{requestId}/accept")
    public ResponseEntity<MatchRequestDto> acceptMatchRequest(@PathVariable String requestId) {
        return ResponseEntity.ok(matchRequestService.acceptMatchRequest(requestId));
    }
    
    @PostMapping("/{requestId}/reject")
    public ResponseEntity<MatchRequestDto> rejectMatchRequest(@PathVariable String requestId) {
        return ResponseEntity.ok(matchRequestService.rejectMatchRequest(requestId));
    }
    
    @GetMapping
    public ResponseEntity<List<MatchRequestDto>> getMatchRequests(
            @RequestParam(required = false, defaultValue = "PENDING") MatchStatus status) {
        
        if (status == MatchStatus.PENDING) {
            return ResponseEntity.ok(matchRequestService.getPendingRequests());
        }
        
        // 다른 상태의 요청은 아직 구현하지 않음
        return ResponseEntity.ok(List.of());
    }
} 