package com.wowelang.chatserver.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.core.mapping.event.BeforeConvertCallback;
import org.springframework.data.mongodb.config.AbstractMongoClientConfiguration;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.connection.SslSettings;
import org.springframework.beans.factory.annotation.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.wowelang.chatserver.model.ChatMessage;
import com.wowelang.chatserver.model.ChatRoom;
import com.wowelang.chatserver.model.MatchRequest;

import java.time.Instant;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.HostnameVerifier;
import java.security.SecureRandom;

@Configuration
@EnableMongoAuditing
public class MongoConfig extends AbstractMongoClientConfiguration {
    
    private static final Logger logger = LoggerFactory.getLogger(MongoConfig.class);

    @Value("${spring.data.mongodb.uri}")
    private String mongoUri;

    @Value("${spring.data.mongodb.database:chatdb}")
    private String database;

    @Override
    protected String getDatabaseName() {
        return database;
    }

    // 모든 인증서를 신뢰하는 SSLSocketFactory 생성
    private SSLSocketFactory createTrustAllSSLSocketFactory() {
        try {
            // 모든 인증서를 신뢰하는 TrustManager 생성
            TrustManager[] trustAllCerts = new TrustManager[] {
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() { 
                        return new X509Certificate[0]; 
                    }
                    public void checkClientTrusted(X509Certificate[] certs, String authType) {}
                    public void checkServerTrusted(X509Certificate[] certs, String authType) {}
                }
            };

            // SSLContext 생성 및 설정
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCerts, new SecureRandom());
            
            logger.info("신뢰 관리자와 SSL 컨텍스트 생성 완료 - 모든 인증서 허용");
            
            return sslContext.getSocketFactory();
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            logger.error("SSL 컨텍스트 초기화 중 오류 발생", e);
            throw new RuntimeException("SSL 설정 중 오류 발생", e);
        }
    }

    @Override
    @Bean
    public MongoClientSettings mongoClientSettings() {
        logger.info("MongoDB 설정 적용 중... URI: {}", mongoUri.replaceAll(":[^/]*@", ":***@"));
        ConnectionString connectionString = new ConnectionString(mongoUri);
        
        // SSLSocketFactory 생성
        SSLSocketFactory sslSocketFactory = createTrustAllSSLSocketFactory();
        
        // 기본 설정 구성
        MongoClientSettings.Builder builder = MongoClientSettings.builder()
            .applyConnectionString(connectionString)
            // SSL 설정: SSL은 활성화하되 인증서 검증은 완전히 비활성화
            .applyToSslSettings(sslBuilder -> {
                sslBuilder.enabled(true)
                          .invalidHostNameAllowed(true)
                          .context(getCustomSSLContext());
                logger.info("SSL 설정 적용됨: 활성화=true, 호스트명검증=false, 커스텀SSL컨텍스트=설정됨");
            });
        
        logger.info("MongoDB 클라이언트 설정 완료");
        return builder.build();
    }
    
    private SSLContext getCustomSSLContext() {
        try {
            TrustManager[] trustAllCerts = new TrustManager[] {
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
                    public void checkClientTrusted(X509Certificate[] certs, String authType) {}
                    public void checkServerTrusted(X509Certificate[] certs, String authType) {}
                }
            };
            
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCerts, new SecureRandom());
            return sslContext;
        } catch (Exception e) {
            logger.error("SSL 컨텍스트 생성 중 오류 발생", e);
            throw new RuntimeException("SSL 컨텍스트 생성 실패", e);
        }
    }

    @Bean
    public BeforeConvertCallback<ChatRoom> chatRoomBeforeConvertCallback() {
        return (entity, collection) -> {
            if (entity.getCreatedAt() == null) {
                entity.setCreatedAt(Instant.now());
            }
            entity.setUpdatedAt(Instant.now());
            return entity;
        };
    }

    @Bean
    public BeforeConvertCallback<ChatMessage> chatMessageBeforeConvertCallback() {
        return (entity, collection) -> {
            if (entity.getCreatedAt() == null) {
                entity.setCreatedAt(Instant.now());
            }
            return entity;
        };
    }

    @Bean
    public BeforeConvertCallback<MatchRequest> matchRequestBeforeConvertCallback() {
        return (entity, collection) -> {
            if (entity.getCreatedAt() == null) {
                entity.setCreatedAt(Instant.now());
            }
            entity.setUpdatedAt(Instant.now());
            return entity;
        };
    }
} 