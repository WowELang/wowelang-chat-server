package com.wowelang.chatserver.service;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.wowelang.chatserver.dto.MatchRequestDto;
import com.wowelang.chatserver.exception.ResourceNotFoundException;
import com.wowelang.chatserver.model.ChatRoom;
import com.wowelang.chatserver.model.MatchRequest;
import com.wowelang.chatserver.model.MatchRequest.MatchStatus;
import com.wowelang.chatserver.repository.MatchRequestRepository;
import com.wowelang.chatserver.util.UserContext;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MatchRequestService {

    private final MatchRequestRepository matchRequestRepository;
    private final ChatRoomService chatRoomService;

    public MatchRequestDto sendMatchRequest(String targetId) {
        String requesterId = UserContext.getUserId();
        
        // 자기 자신에게 요청 불가
        if (requesterId.equals(targetId)) {
            throw new IllegalArgumentException("Cannot send match request to yourself");
        }
        
        // 이미 존재하는 요청인지 확인
        MatchRequest existingRequest = matchRequestRepository.findByRequesterIdAndTargetId(requesterId, targetId)
                .orElse(null);
        
        if (existingRequest != null) {
            if (existingRequest.getStatus() == MatchStatus.PENDING) {
                return convertToDto(existingRequest);
            } else {
                existingRequest.setStatus(MatchStatus.PENDING);
                existingRequest.setUpdatedAt(Instant.now());
                return convertToDto(matchRequestRepository.save(existingRequest));
            }
        }
        
        // 상대방이 이미 요청했는지 확인
        MatchRequest reverseRequest = matchRequestRepository.findByRequesterIdAndTargetId(targetId, requesterId)
                .orElse(null);
        
        if (reverseRequest != null && reverseRequest.getStatus() == MatchStatus.PENDING) {
            // 상대방이 이미 요청한 경우 자동 수락
            return acceptMatchRequest(reverseRequest.getId());
        }
        
        // 새 요청 생성
        MatchRequest newRequest = MatchRequest.builder()
                .requesterId(requesterId)
                .targetId(targetId)
                .status(MatchStatus.PENDING)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        
        return convertToDto(matchRequestRepository.save(newRequest));
    }
    
    public MatchRequestDto acceptMatchRequest(String requestId) {
        String userId = UserContext.getUserId();
        
        MatchRequest request = matchRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Match request not found"));
        
        // 대상자만 수락 가능
        if (!request.getTargetId().equals(userId)) {
            throw new IllegalArgumentException("Only the target user can accept the match request");
        }
        
        // 이미 처리된 요청은 다시 처리 불가
        if (request.getStatus() != MatchStatus.PENDING) {
            throw new IllegalArgumentException("Match request already processed");
        }
        
        // 채팅방 생성
        ChatRoom chatRoom = chatRoomService.createRoom(
                Arrays.asList(request.getRequesterId(), request.getTargetId()));
        
        // 요청 상태 업데이트
        request.setStatus(MatchStatus.ACCEPTED);
        request.setChatRoomId(chatRoom.getId());
        request.setUpdatedAt(Instant.now());
        
        return convertToDto(matchRequestRepository.save(request));
    }
    
    public MatchRequestDto rejectMatchRequest(String requestId) {
        String userId = UserContext.getUserId();
        
        MatchRequest request = matchRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Match request not found"));
        
        // 대상자만 거절 가능
        if (!request.getTargetId().equals(userId)) {
            throw new IllegalArgumentException("Only the target user can reject the match request");
        }
        
        // 이미 처리된 요청은 다시 처리 불가
        if (request.getStatus() != MatchStatus.PENDING) {
            throw new IllegalArgumentException("Match request already processed");
        }
        
        // 요청 상태 업데이트
        request.setStatus(MatchStatus.REJECTED);
        request.setUpdatedAt(Instant.now());
        
        return convertToDto(matchRequestRepository.save(request));
    }
    
    public List<MatchRequestDto> getPendingRequests() {
        String userId = UserContext.getUserId();
        
        List<MatchRequest> requests = matchRequestRepository.findByUserIdAndStatus(userId, MatchStatus.PENDING);
        
        return requests.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    private MatchRequestDto convertToDto(MatchRequest request) {
        return MatchRequestDto.builder()
                .id(request.getId())
                .requesterId(request.getRequesterId())
                .targetId(request.getTargetId())
                .status(request.getStatus())
                .createdAt(request.getCreatedAt())
                .updatedAt(request.getUpdatedAt())
                .chatRoomId(request.getChatRoomId())
                .build();
    }
} 