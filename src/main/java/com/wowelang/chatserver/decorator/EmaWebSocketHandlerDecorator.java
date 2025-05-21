package com.wowelang.chatserver.decorator;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.WebSocketHandlerDecorator;

/**
 * 웹소켓 핸들러 데코레이터로 STOMP 프로토콜을 수동으로 테스트할 수 있게 해줍니다.
 * 클라이언트 측에서 null 문자가 없이 전송된 메시지에 자동으로 null 문자를 추가합니다.
 * 또한 필요한 경우 캐리지 리턴을 추가합니다.
 */
public class EmaWebSocketHandlerDecorator extends WebSocketHandlerDecorator {

    private static final Logger logger = LoggerFactory.getLogger(EmaWebSocketHandlerDecorator.class);

    public EmaWebSocketHandlerDecorator(WebSocketHandler webSocketHandler) {
        super(webSocketHandler);
    }

    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
        super.handleMessage(session, updateBodyIfNeeded(message));
    }

    /**
     * 지정된 메시지의 내용을 업데이트합니다. 메시지가 TextMessage이고 끝에 null 문자가 없는 경우에만 
     * 업데이트됩니다. 명령이 본문을 필요로 하지 않는 경우 캐리지 리턴이 누락되었다면 추가됩니다.
     */
    private WebSocketMessage<?> updateBodyIfNeeded(WebSocketMessage<?> message) {
        if (!(message instanceof TextMessage) || ((TextMessage) message).getPayload().endsWith("\u0000")) {
            return message;
        }

        String payload = ((TextMessage) message).getPayload();
        
        logger.debug("메시지 페이로드: '{}'", payload);

        final Optional<StompCommand> stompCommand = getStompCommand(payload);

        if (!stompCommand.isPresent()) {
            logger.debug("STOMP 명령을 인식할 수 없습니다. 원본 메시지 반환");
            return message;
        }

        logger.debug("STOMP 명령 감지: {}", stompCommand.get());

        if (!stompCommand.get().isBodyAllowed() && !payload.endsWith("\n\n")) {
            logger.debug("본문이 불필요한 명령에 개행 문자 추가");
            if (payload.endsWith("\n")) {
                payload += "\n";
            } else {
                payload += "\n\n";
            }
        }

        payload += "\u0000";
        
        logger.debug("업데이트된 페이로드: '{}'", payload);

        return new TextMessage(payload);
    }

    /**
     * 지정된 페이로드와 연관된 STOMP 명령을 반환합니다.
     */
    private Optional<StompCommand> getStompCommand(String payload) {
        final int firstCarriageReturn = payload.indexOf('\n');

        if (firstCarriageReturn < 0) {
            return Optional.empty();
        }

        try {
            return Optional.of(
                    StompCommand.valueOf(payload.substring(0, firstCarriageReturn))
            );
        } catch (IllegalArgumentException e) {
            logger.trace("STOMP 명령 파싱 중 오류 발생.", e);

            return Optional.empty();
        }
    }
} 