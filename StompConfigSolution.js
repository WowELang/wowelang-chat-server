import { Client } from '@stomp/stompjs';

/**
 * React Native 앱에서 STOMP 클라이언트 생성을 위한 함수
 * 
 * @param {string} url - WebSocket 서버 URL (예: ws://example.com/chat-websocket)
 * @param {string} userId - 사용자 ID
 * @param {Function} onConnect - 연결 성공 시 콜백
 * @param {Function} onDisconnect - 연결 해제 시 콜백
 * @param {Function} onError - 오류 발생 시 콜백
 * @param {Function} debug - 디버그 로그 콜백
 * @returns {Client} - 설정된 StompJS 클라이언트 인스턴스
 */
export function createStompClient(url, userId, onConnect, onDisconnect, onError, debug) {
  // StompJS 클라이언트 설정
  const client = new Client({
    brokerURL: url,
    connectHeaders: {
      'X-User-Id': userId
    },
    // 중요: 자동으로 NULL 종결자를 추가하도록 설정
    // 이 옵션이 없으면 "Incomplete STOMP frame" 오류가 발생할 수 있음
    appendMissingNUL: true,
    
    // 연결이 끊어졌을 때 재연결 시도 간격(ms)
    reconnectDelay: 5000,
    
    // STOMP 프로토콜 버전 설정
    stompVersions: new Map([
      ['v12', '1.2'],
      ['v11', '1.1'],
      ['v10', '1.0']
    ]),
    
    // 디버그 로그 설정
    debug: debug || function(str) {
      console.log(`STOMP: ${str}`);
    },
    
    // 하트비트 설정 (단위: ms)
    heartbeatIncoming: 4000,
    heartbeatOutgoing: 4000,
  });

  // 이벤트 핸들러 등록
  client.onConnect = function(frame) {
    console.log('STOMP 서버에 연결되었습니다!');
    if (onConnect) onConnect(frame);
  };

  client.onStompError = function(frame) {
    console.error(`STOMP 오류: ${frame.headers['message']}`);
    console.error(`상세 내용: ${frame.body}`);
    if (onError) onError(frame);
  };

  client.onWebSocketError = function(event) {
    console.error('웹소켓 오류가 발생했습니다.');
    if (onError) onError(event);
  };

  client.onWebSocketClose = function(event) {
    console.log(`웹소켓 연결이 닫혔습니다. 코드: ${event.code}, 이유: ${event.reason || '알 수 없음'}`);
    if (onDisconnect) onDisconnect(event);
  };

  return client;
}

/**
 * 사용 예제:
 * 
 * import { createStompClient } from './StompConfigSolution';
 * 
 * // 클라이언트 생성
 * const client = createStompClient(
 *   'ws://example.com/chat-websocket',
 *   'user123',
 *   () => console.log('연결됨'),
 *   () => console.log('연결 해제됨'),
 *   (error) => console.error('오류:', error)
 * );
 * 
 * // 연결 시작
 * client.activate();
 * 
 * // 연결 해제
 * // client.deactivate();
 */ 