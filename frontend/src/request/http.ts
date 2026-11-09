import axios, { AxiosError } from 'axios';
import type { ApiResponse } from './types';

/** 业务错误: 携带后端 code(40001/40101/...), 页面可据此分支处理 */
export class ApiError extends Error {
  readonly code: number;

  constructor(code: number, message: string) {
    super(message);
    this.name = 'ApiError';
    this.code = code;
  }
}

/**
 * 解包统一响应: code !== 200 一律转成 ApiError
 * 抽成纯函数便于单测(拦截器里也复用)
 */
export function unwrap<T>(payload: ApiResponse<T>): T {
  if (!payload || typeof payload.code !== 'number') {
    throw new ApiError(50000, '响应格式异常');
  }
  if (payload.code !== 200) {
    throw new ApiError(payload.code, payload.message || '请求失败');
  }
  return payload.data;
}

export const TOKEN_KEY = 'notehub_token';

export function getToken(): string | null {
  return localStorage.getItem(TOKEN_KEY);
}

export function setToken(token: string | null): void {
  if (token) {
    localStorage.setItem(TOKEN_KEY, token);
  } else {
    localStorage.removeItem(TOKEN_KEY);
  }
}

/** 未登录事件: 页面/布局监听后弹登录框(避免 request 层直接依赖 UI) */
export const UNAUTHORIZED_EVENT = 'notehub:unauthorized';

const http = axios.create({
  baseURL: '/',
  timeout: 15000,
});

http.interceptors.request.use((config) => {
  const token = getToken();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

http.interceptors.response.use(
  (response) => response,
  (error: AxiosError<ApiResponse<unknown>>) => {
    // 后端业务错误一般 HTTP 200; 这里兜底网络/500 场景
    const body = error.response?.data;
    if (body && typeof body.code === 'number') {
      return Promise.reject(new ApiError(body.code, body.message || '请求失败'));
    }
    if (error.code === 'ECONNABORTED') {
      return Promise.reject(new ApiError(50000, '请求超时, 请稍后重试'));
    }
    return Promise.reject(new ApiError(50000, '网络异常, 请检查后端是否启动'));
  },
);

/** 统一请求出口: 自动解包 data, 失败抛 ApiError */
export async function request<T>(config: Parameters<typeof http.request>[0]): Promise<T> {
  const response = await http.request<ApiResponse<T>>(config);
  const data = unwrap(response.data);
  // 需要登录的接口在后端返回 40101: 清 token 并广播事件
  return data;
}

/** 捕获未登录并广播(供 @NeedLogin 接口的调用方使用) */
export function handleAuthError(error: unknown): boolean {
  if (error instanceof ApiError && (error.code === 40101 || error.code === 40102)) {
    if (error.code === 40101) {
      setToken(null);
      window.dispatchEvent(new CustomEvent(UNAUTHORIZED_EVENT));
    }
    return true;
  }
  return false;
}

export default http;
