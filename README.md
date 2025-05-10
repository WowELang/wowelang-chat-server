# Wowelang 1:1 Chat Server

Spring Boot 기반의 1:1 채팅 서버 애플리케이션입니다. 한국어-외국어 언어 교환 앱을 위한 실시간 채팅 시스템을 제공합니다.

## 기술 스택

- **런타임**: Spring Boot 3.4.4 + Java 21 (Gradle)
- **실시간 통신**: Spring WebSocket + STOMP (SockJS 폴백)
- **데이터베이스**: AWS DocumentDB (MongoDB 호환)
- **저장소**: AWS S3 (`wowelang-chat-media-dev` 버킷, ap-northeast-2 리전)
- **CI/CD**: GitHub Actions → Docker 이미지 → AWS EC2
- **인증**: 현재는 `X-User-Id` 헤더 검사 (JWT를 위한 확장 가능한 필터 제공)
- **푸시 알림**: 예정 - Firebase Cloud Messaging (현재는 도메인 이벤트만 구현)

## 주요 기능

### 매칭 및 채팅방 생성
외부 매칭 서비스는 후보 사용자 ID만 추천합니다. 모든 매치메이킹 로직은 이 프로젝트 내에 구현되어 있습니다.

| 작업 | 엔드포인트 | 설명 |
|------|------------|------|
| 매치 요청 전송 | `POST /matches/{targetId}` | `MatchRequest`(PENDING 상태) 생성 |
| 요청 수락/거절 | `POST /matches/{id}/accept` / `.../reject` | 수락 시 **ChatRoom** 생성 (정확히 2명의 참여자) |
| 대기 중 요청 목록 | `GET /matches?status=PENDING` | |

**비즈니스 규칙**: 각 사용자 쌍은 **최대 1개**의 채팅방을 소유합니다. 중복 요청은 멱등적으로 처리됩니다.

### 채팅 메시징
- 1:1 채팅방만 지원 - 두 사용자 ID는 `ChatRoom.participants[2]`에 저장됩니다.
- STOMP 연결
  - 클라이언트 전송 → `/app/chat.send.{roomId}`
  - 브로커 브로드캐스트 → `/topic/chat.{roomId}`
- 메시지 유형
  - `TEXT`, `IMAGE`, `CORRECTION` (HelloTalk 스타일 교정)
- **제한사항**
  - 최대 텍스트 길이 = **2,000자** (UTF-8)
  - 최대 첨부 파일 크기 = **10MB** (사전 서명된 URL 정책 + 서버 측 유효성 검사)
- 이미지 업로드 흐름
  1. `POST /attachments/presign?mime=image/jpeg` → 사전 서명된 **PUT** URL (15분 후 만료)
  2. 클라이언트가 업로드 후 `{ type:"IMAGE", s3Key:"..." }` 포함 메시지 전송

### 메시지 액션
- **전송**: 저장 + 브로드캐스트
- **삭제**: 소프트 삭제 (deleted=true로 설정)
- **페이지 처리**: `GET /rooms/{id}/messages?before=<ISO8601>&size=<N>` - 최신 순으로 반환
  - 기본값: size = 30, before = 현재; N ≤ 100

### 채팅방 라이프사이클
참여자 중 한 명이 채팅방을 나가면 (`DELETE /rooms/{id}`) → deleted=true로 플래그 설정; TTL 30일

### 조회
- `GET /rooms`: 발신자의 활성 채팅방 목록, 마지막 메시지 미리보기 및 읽지 않은 메시지 수 포함
- `GET /rooms/{id}/messages`: 위에서 설명한 대로 페이지 처리된 메시지 이력

## 사용 방법

### 프로젝트 설정 및 실행

```bash
# 프로젝트 복제
git clone https://github.com/your-org/wowelang-chat-server.git
cd wowelang-chat-server

# Gradle로 빌드
./gradlew build

# 애플리케이션 실행
./gradlew bootRun
```

### 환경 변수

```
# 필수 환경 변수
MONGODB_URI=mongodb://username:password@your-docdb-cluster.cluster-xxx.region.docdb.amazonaws.com:27017/chatserver?tls=true&replicaSet=rs0&readPreference=secondaryPreferred&retryWrites=false
AWS_ACCESS_KEY_ID=...
AWS_SECRET_ACCESS_KEY=...

# 선택적 환경 변수
SPRING_PROFILES_ACTIVE=prod  # 프로덕션 환경 설정 활성화
```

### AWS DocumentDB 연결 정보

프로덕션 환경에서는 Amazon DocumentDB를 사용합니다. DocumentDB 연결을 위해서는 다음 형식의 URI가 필요합니다:

```
mongodb://username:password@your-docdb-cluster.cluster-xxx.region.docdb.amazonaws.com:27017/dbname?tls=true&replicaSet=rs0&readPreference=secondaryPreferred&retryWrites=false
```

중요 파라미터:
- `tls=true`: DocumentDB는 TLS 연결을 요구합니다.
- `replicaSet=rs0`: DocumentDB 클러스터에 필요합니다.
- `readPreference=secondaryPreferred`: 읽기 성능 최적화
- `retryWrites=false`: DocumentDB 호환성 설정

이 URI는 GitHub Secret `MONGODB_URI`에 저장되어 CI/CD 파이프라인에서 환경 변수로 주입됩니다.

### Docker 실행

```bash
docker run -d --name chat-server \
  -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e MONGODB_URI=${MONGODB_URI} \
  -e AWS_ACCESSKEY=${AWS_ACCESS_KEY_ID} \
  -e AWS_SECRETKEY=${AWS_SECRET_ACCESS_KEY} \
  your-docker-username/wowelang-chat-server:latest
```

## API 문서

API는 다음 방식으로 사용할 수 있습니다:

### Matching API
```
# 매치 요청 전송
POST /matches/{targetId}

# 매치 요청 수락
POST /matches/{requestId}/accept

# 매치 요청 거절
POST /matches/{requestId}/reject

# 대기 중인 매치 요청 조회
GET /matches?status=PENDING
```

### Room API
```
# 채팅방 목록 조회
GET /rooms

# 채팅방 삭제
DELETE /rooms/{roomId}
```

### Message API
```
# 메시지 목록 조회
GET /rooms/{roomId}/messages?before=2023-01-01T12:00:00Z&size=50

# 메시지 삭제
DELETE /rooms/{roomId}/messages/{messageId}
```

### Attachment API
```
# 첨부 파일 업로드를 위한 사전 서명된 URL 요청
POST /attachments/presign?mime=image/jpeg
```

## 테스트

```bash
# 모든 테스트 실행
./gradlew test

# 특정 테스트 실행
./gradlew test --tests "com.wowelang.chatserver.integration.ChatServerIntegrationTest"
```

## 배포

배포는 GitHub Actions를 통해 자동화되어 있습니다. `main` 브랜치에 변경 사항을 푸시하면 다음 단계가 수행됩니다:

1. Java 빌드 및 테스트
2. Docker 이미지 빌드 및 푸시
3. AWS EC2 인스턴스에 SSH 연결 및 배포 

## CI/CD 트리거

마지막 워크플로우 트리거: 2025-05-10 10:30 