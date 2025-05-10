package com.wowelang.chatserver.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.wowelang.chatserver.dto.PresignedUrlRequest;
import com.wowelang.chatserver.dto.PresignedUrlResponse;
import com.wowelang.chatserver.service.S3Service;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/attachments")
@RequiredArgsConstructor
public class AttachmentController {

    private final S3Service s3Service;
    
    /**
     * 파일 업로드용 PUT Presigned URL을 생성합니다.
     * 
     * @param request MIME 타입 정보를 담은 요청 객체
     * @return Presigned URL 정보를 담은 응답 객체
     */
    @PostMapping("/presign")
    public ResponseEntity<PresignedUrlResponse> getPresignedUrl(@Valid @RequestBody PresignedUrlRequest request) {
        return ResponseEntity.ok(s3Service.generatePresignedUrl(request.getMimeType()));
    }
    
    /**
     * 파일 다운로드/조회용 GET Presigned URL을 생성합니다.
     * 이 엔드포인트는 이미 업로드된 파일에 접근하기 위한 임시 URL을 제공합니다.
     * 
     * @param s3Key 조회할 파일의 S3 객체 키
     * @return Presigned URL 정보를 담은 응답 객체
     */
    @GetMapping("/presigned-url")
    public ResponseEntity<PresignedUrlResponse> getPresignedGetUrl(@RequestParam("s3Key") String s3Key) {
        return ResponseEntity.ok(s3Service.generatePresignedGetUrl(s3Key));
    }
} 