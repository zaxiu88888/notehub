import { UNAUTHORIZED_EVENT } from '@/request/http';

/**
 * 需要登录的操作前置校验: 未登录时广播事件(布局会弹登录框)
 * @returns 是否已登录
 */
export function requireLogin(token: string | null): boolean {
  if (token) return true;
  window.dispatchEvent(new CustomEvent(UNAUTHORIZED_EVENT));
  return false;
}
