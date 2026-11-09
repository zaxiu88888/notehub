import { Client } from '@stomp/stompjs';
import type { UnreadCount } from '@/domain/note/types';

/**
 * WebSocket 地址: 开发期走 Vite /ws 代理, 生产同源; https 自动升级 wss
 */
export function buildWsUrl(origin: { protocol: string; host: string } = window.location): string {
  const protocol = origin.protocol === 'https:' ? 'wss:' : 'ws:';
  return `${protocol}//${origin.host}/ws`;
}

export interface UnreadSocketOptions {
  token: string;
  onUnread: (counts: UnreadCount) => void;
  /** 每次连接/重连成功(用于拉一次计数补齐断线期间的消息) */
  onConnected?: () => void;
  onDisconnected?: () => void;
}

export interface UnreadSocket {
  disconnect: () => void;
}

/**
 * 订阅 /user/queue/unread; JWT 放在 STOMP CONNECT 头里
 * 断线自动重连(5s), 认证失败(ERROR 帧)则停止重连
 */
export function connectUnreadSocket({
  token,
  onUnread,
  onConnected,
  onDisconnected,
}: UnreadSocketOptions): UnreadSocket {
  const client = new Client({
    brokerURL: buildWsUrl(),
    connectHeaders: { Authorization: `Bearer ${token}` },
    reconnectDelay: 5000,
    heartbeatIncoming: 10_000,
    heartbeatOutgoing: 10_000,
    debug: () => undefined,
  });

  client.onConnect = () => {
    client.subscribe('/user/queue/unread', (message) => {
      try {
        onUnread(JSON.parse(message.body) as UnreadCount);
      } catch {
        // 忽略非法载荷, 角标保持上一次值
      }
    });
    onConnected?.();
  };
  client.onWebSocketClose = () => onDisconnected?.();
  client.onStompError = () => {
    // 鉴权失败等协议错误: 不无限重连, 等待用户重新登录
    void client.deactivate();
  };

  client.activate();
  return { disconnect: () => void client.deactivate() };
}
