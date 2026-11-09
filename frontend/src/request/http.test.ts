import { describe, expect, it, beforeEach } from 'vitest';
import { ApiError, TOKEN_KEY, getToken, setToken, unwrap } from '@/request/http';

describe('unwrap 统一响应解包', () => {
  it('code=200 时返回 data', () => {
    expect(unwrap({ code: 200, message: 'success', data: { id: 1 } })).toEqual({ id: 1 });
  });

  it('code!=200 时抛出带业务码的 ApiError', () => {
    try {
      unwrap({ code: 40101, message: '用户未登录', data: null });
      throw new Error('应当抛错');
    } catch (e) {
      expect(e).toBeInstanceOf(ApiError);
      expect((e as ApiError).code).toBe(40101);
      expect((e as ApiError).message).toBe('用户未登录');
    }
  });

  it('响应体格式异常时按 50000 处理', () => {
    // @ts-expect-error 故意传非法结构
    expect(() => unwrap({ foo: 'bar' })).toThrowError(ApiError);
  });
});

describe('token 存储', () => {
  beforeEach(() => localStorage.clear());

  it('setToken 写入 / 传 null 清除', () => {
    setToken('abc');
    expect(localStorage.getItem(TOKEN_KEY)).toBe('abc');
    expect(getToken()).toBe('abc');

    setToken(null);
    expect(localStorage.getItem(TOKEN_KEY)).toBeNull();
    expect(getToken()).toBeNull();
  });
});
