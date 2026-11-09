import { describe, expect, it, vi } from 'vitest';
import type { UnreadCount } from '@/domain/note/types';

vi.mock('@stomp/stompjs', () => {
  class MockClient {
    static instances: MockClient[] = [];
    config: Record<string, unknown>;
    onConnect?: () => void;
    onWebSocketClose?: () => void;
    onStompError?: () => void;
    subscribe = vi.fn();
    activate = vi.fn();
    deactivate = vi.fn();

    constructor(config: Record<string, unknown>) {
      this.config = config;
      MockClient.instances.push(this);
    }
  }
  return { Client: MockClient };
});

import { Client } from '@stomp/stompjs';
import { buildWsUrl, connectUnreadSocket } from './socket';

const MockClient = Client as unknown as { instances: Array<Record<string, any>> };

describe('buildWsUrl', () => {
  it('http → ws, https → wss, 路径 /ws', () => {
    expect(buildWsUrl({ protocol: 'http:', host: 'localhost:5173' })).toBe(
      'ws://localhost:5173/ws',
    );
    expect(buildWsUrl({ protocol: 'https:', host: 'notehub.dev' })).toBe(
      'wss://notehub.dev/ws',
    );
  });
});

describe('connectUnreadSocket', () => {
  it('携带 JWT 连接, 订阅个人队列, 断开时 deactivate', () => {
    const onUnread = vi.fn();
    const onConnected = vi.fn();
    const socket = connectUnreadSocket({ token: 'jwt-token', onUnread, onConnected });

    const client = MockClient.instances[0];
    expect(client.config.connectHeaders).toEqual({ Authorization: 'Bearer jwt-token' });
    expect(client.activate).toHaveBeenCalled();

    client.onConnect?.();
    expect(client.subscribe).toHaveBeenCalledWith('/user/queue/unread', expect.any(Function));
    expect(onConnected).toHaveBeenCalled();

    const counts: UnreadCount = { total: 2, like: 1, comment: 1, system: 0 };
    const handler = client.subscribe.mock.calls[0][1] as (message: { body: string }) => void;
    handler({ body: JSON.stringify(counts) });
    expect(onUnread).toHaveBeenCalledWith(counts);

    handler({ body: 'not-json' }); // 非法载荷不抛异常
    socket.disconnect();
    expect(client.deactivate).toHaveBeenCalled();
  });
});
