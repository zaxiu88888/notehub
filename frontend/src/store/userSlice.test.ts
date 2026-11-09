import { describe, expect, it, beforeEach } from 'vitest';
import { configureStore } from '@reduxjs/toolkit';
import userReducer, { clearAuth, logout, fetchWhoamiThunk, loginThunk } from './userSlice';
import { TOKEN_KEY, setToken } from '@/request/http';
import type { UserVO } from '@/domain/user/types';

function makeStore() {
  return configureStore({ reducer: { user: userReducer } });
}

const demoUser: UserVO = {
  userId: 2,
  account: 'demo',
  username: '刷题小白',
  email: 'demo@notehub.dev',
  gender: 0,
  birthday: null,
  avatarUrl: null,
  school: null,
  signature: null,
  isAdmin: 0,
  isBanned: 0,
  lastLoginAt: null,
  createdAt: '2026-11-01T00:00:00',
};

describe('userSlice', () => {
  beforeEach(() => localStorage.clear());

  it('logout / clearAuth 清空会话与本地 token', () => {
    setToken('t1');
    const store = makeStore();
    store.dispatch({ type: 'user/whoami/fulfilled', payload: demoUser });
    expect(store.getState().user.user?.username).toBe('刷题小白');

    store.dispatch(logout());
    expect(store.getState().user.token).toBeNull();
    expect(store.getState().user.user).toBeNull();
    expect(localStorage.getItem(TOKEN_KEY)).toBeNull();

    setToken('t2');
    store.dispatch(clearAuth());
    expect(localStorage.getItem(TOKEN_KEY)).toBeNull();
  });

  it('登录成功: 写入 token 与 loading 归位', async () => {
    const store = makeStore();
    const promise = store.dispatch({
      type: loginThunk.fulfilled.type,
      payload: { userId: 2, token: 'jwt-token' },
    });
    await promise;
    expect(store.getState().user.token).toBe('jwt-token');
    expect(store.getState().user.loading).toBe(false);
  });

  it('拉取资料成功: 填充 user', async () => {
    const store = makeStore();
    await store.dispatch({ type: fetchWhoamiThunk.fulfilled.type, payload: demoUser });
    expect(store.getState().user.user?.userId).toBe(2);
  });
});
