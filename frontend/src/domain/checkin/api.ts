import { request } from '@/request/http';
import type { CheckinStatusVO } from './types';

export function getCheckinStatus() {
  return request<CheckinStatusVO>({ url: '/api/checkin/status', method: 'GET' });
}

export function checkin() {
  return request<CheckinStatusVO>({ url: '/api/checkin', method: 'POST' });
}
