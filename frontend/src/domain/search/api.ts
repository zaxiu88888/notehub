import { request } from '@/request/http';
import type { PageData } from '@/request/types';
import type { NoteVO } from '@/domain/note/types';
import type { UserVO } from '@/domain/user/types';

/** I1 搜索 */
export function searchNotes(params: { keyword: string; page?: number; pageSize?: number }) {
  return request<PageData<NoteVO>>({ url: '/api/search/notes', method: 'GET', params });
}

export function searchUsers(params: { keyword: string; page?: number; pageSize?: number }) {
  return request<PageData<UserVO>>({ url: '/api/search/users', method: 'GET', params });
}
