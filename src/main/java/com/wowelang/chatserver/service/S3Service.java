package com.wowelang.chatserver.service;

import java.time.Duration;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.wowelang.chatserver.dto.PresignedUrlResponse;

import lombok.RequiredArgsConstructor;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

@Service
@RequiredArgsConstructor
public class S3Service {

    private final S3Presigner s3Presigner;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    @Value("${aws.s3.presigned-url-expiration}")
    private int presignedUrlExpirationMinutes;

    /**
     * 파일 업로드용 PUT Presigned URL을 생성합니다.
     * 
     * @param mimeType 업로드할 파일의 MIME 타입
     * @return Presigned URL 정보를 담은 응답 객체
     */
    public PresignedUrlResponse generatePresignedUrl(String mimeType) {
        String fileExtension = getFileExtensionFromMimeType(mimeType);
        String s3Key = "uploads/" + UUID.randomUUID() + fileExtension;

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(s3Key)
                .contentType(mimeType)
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(presignedUrlExpirationMinutes))
                .putObjectRequest(putObjectRequest)
                .build();

        PresignedPutObjectRequest presignedRequest = s3Presigner.presignPutObject(presignRequest);
        
        return PresignedUrlResponse.builder()
                .url(presignedRequest.url().toString())
                .s3Key(s3Key)
                .expiresInMinutes(presignedUrlExpirationMinutes)
                .build();
    }
    
    /**
     * 파일 다운로드/조회용 GET Presigned URL을 생성합니다.
     * 
     * @param s3Key 조회할 파일의 S3 객체 키
     * @return Presigned URL 정보를 담은 응답 객체
     */
    public PresignedUrlResponse generatePresignedGetUrl(String s3Key) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(s3Key)
                .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(presignedUrlExpirationMinutes))
                .getObjectRequest(getObjectRequest)
                .build();

        PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(presignRequest);
        
        return PresignedUrlResponse.builder()
                .url(presignedRequest.url().toString())
                .s3Key(s3Key)
                .expiresInMinutes(presignedUrlExpirationMinutes)
                .build();
    }

    private String getFileExtensionFromMimeType(String mimeType) {
        return switch (mimeType.toLowerCase()) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/gif" -> ".gif";
            case "image/webp" -> ".webp";
            default -> "";
        };
    }
} 