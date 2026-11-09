import { request } from '@/request/http';
import type { TagVO } from './types';

export function getTags(limit = 20) {
  return request<TagVO[]>({ url: '/api/tags', method: 'GET', params: { limit } });
}
