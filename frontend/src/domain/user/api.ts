import { request } from '@/request/http';
import type { LoginBody, LoginUserVO, RegisterBody, UpdateUserBody, UserVO } from './types';

/** 发送注册验证码(dev 模式会返回 devCode 便于联调) */
export function sendVerifyCode(email: string) {
  return request<{ sent: boolean; devCode?: string }>({
    url: '/api/email/verify-code',
    method: 'GET',
    params: { email },
  });
}

export function register(body: RegisterBody) {
  return request<LoginUserVO>({ url: '/api/users', method: 'POST', data: body });
}

export function login(body: LoginBody) {
  return request<LoginUserVO>({ url: '/api/users/login', method: 'POST', data: body });
}

export function whoami() {
  return request<UserVO>({ url: '/api/users/whoami', method: 'POST' });
}

export function updateMe(body: UpdateUserBody) {
  return request<UserVO>({ url: '/api/users/me', method: 'PATCH', data: body });
}

export function uploadAvatar(file: File) {
  const form = new FormData();
  form.append('file', file);
  return request<{ avatarUrl: string }>({
    url: '/api/users/avatar',
    method: 'POST',
    data: form,
  });
}

export function getPublicProfile(userId: number) {
  return request<UserVO>({ url: `/api/users/${userId}`, method: 'GET' });
}
