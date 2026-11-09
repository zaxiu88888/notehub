import { describe, expect, it } from 'vitest';
import reducer, { resetNotification, setConnected, setUnread } from './notificationSlice';

describe('notificationSlice', () => {
  it('初始未读为 0, 未连接', () => {
    const state = reducer(undefined, { type: '@@init' });
    expect(state.unread).toEqual({ total: 0, like: 0, comment: 0, system: 0 });
    expect(state.connected).toBe(false);
  });

  it('设置未读计数与连接状态', () => {
    let state = reducer(undefined, setUnread({ total: 3, like: 2, comment: 1, system: 0 }));
    state = reducer(state, setConnected(true));
    expect(state.unread.total).toBe(3);
    expect(state.connected).toBe(true);
  });

  it('退出登录重置', () => {
    let state = reducer(undefined, setUnread({ total: 5, like: 5, comment: 0, system: 0 }));
    state = reducer(state, resetNotification());
    expect(state.unread.total).toBe(0);
    expect(state.connected).toBe(false);
  });
});
