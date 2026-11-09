import http, { ApiError, request } from '@/request/http';
import type { ApiResponse, PageData } from '@/request/types';
import type {
  CollectionVO,
  CommentVO,
  HeatmapVO,
  MessageVO,
  NoteStatsVO,
  NoteVO,
  RankNoteVO,
  RankPeriod,
  UnreadCount,
} from './types';

// ---------- 笔记 ----------

export function getNotes(params: {
  questionId?: number;
  authorId?: number;
  sort?: 'latest' | 'hot';
  page?: number;
  pageSize?: number;
}) {
  return request<PageData<NoteVO>>({ url: '/api/notes', method: 'GET', params });
}

export function getNote(noteId: number) {
  return request<NoteVO>({ url: `/api/notes/${noteId}`, method: 'GET' });
}

export function createNote(body: { questionId: number; content: string }) {
  return request<{ noteId: number }>({ url: '/api/notes', method: 'POST', data: body });
}

export function updateNote(noteId: number, content: string) {
  return request<void>({ url: `/api/notes/${noteId}`, method: 'PATCH', data: { content } });
}

export function deleteNote(noteId: number) {
  return request<void>({ url: `/api/notes/${noteId}`, method: 'DELETE' });
}

export function getMyNoteId(questionId: number) {
  return request<{ noteId: number }>({
    url: '/api/notes/mine',
    method: 'GET',
    params: { questionId },
  });
}

// ---------- I2 数据可视化 ----------

export function getHeatmap(year?: number) {
  return request<HeatmapVO>({
    url: '/api/notes/heatmap',
    method: 'GET',
    params: year ? { year } : undefined,
  });
}

export function getNoteRanklist(params: { period?: RankPeriod; limit?: number }) {
  return request<RankNoteVO[]>({
    url: '/api/notes/ranklist',
    method: 'GET',
    params,
  });
}

export function getNoteStats() {
  return request<NoteStatsVO>({ url: '/api/notes/top3count', method: 'GET' });
}

/** 导出 Markdown 附件: 不走统一 JSON 解包, 直接触发浏览器下载 */
export async function downloadNotes(noteId?: number): Promise<void> {
  const response = await http.get<Blob>('/api/notes/download', {
    params: noteId ? { noteId } : undefined,
    responseType: 'blob',
  });
  const blob = response.data;
  // 后端业务错误仍是 JSON(HTTP 200), blob 里能识别出来
  if (blob.type.includes('application/json')) {
    const payload = JSON.parse(await blob.text()) as ApiResponse<unknown>;
    throw new ApiError(payload.code ?? 50000, payload.message || '导出失败');
  }
  const disposition = String(response.headers['content-disposition'] ?? '');
  const matched = disposition.match(/filename="?([^";]+)"?/);
  const filename = matched?.[1] ?? (noteId ? `note-${noteId}.md` : 'notehub-notes.md');

  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = filename;
  document.body.appendChild(link);
  link.click();
  link.remove();
  // 立即 revoke 在部分浏览器会中断下载, 留出触发窗口
  setTimeout(() => URL.revokeObjectURL(url), 1000);
}

// ---------- 点赞 ----------

export function likeNote(noteId: number) {
  return request<{ likeCount: number }>({ url: `/api/like/note/${noteId}`, method: 'POST' });
}

export function unlikeNote(noteId: number) {
  return request<void>({ url: `/api/like/note/${noteId}`, method: 'DELETE' });
}

export function likeComment(commentId: number) {
  return request<void>({ url: `/api/like/comment/${commentId}`, method: 'POST' });
}

export function unlikeComment(commentId: number) {
  return request<void>({ url: `/api/like/comment/${commentId}`, method: 'DELETE' });
}

// ---------- 评论 ----------

export function getComments(params: { noteId: number; page?: number; pageSize?: number }) {
  return request<PageData<CommentVO>>({ url: '/api/comments', method: 'GET', params });
}

export function postComment(body: { noteId: number; parentId?: number; content: string }) {
  return request<{ commentId: number }>({ url: '/api/comments', method: 'POST', data: body });
}

export function editComment(commentId: number, content: string) {
  return request<void>({
    url: `/api/comments/${commentId}`,
    method: 'PATCH',
    data: { content },
  });
}

export function removeComment(commentId: number) {
  return request<void>({ url: `/api/comments/${commentId}`, method: 'DELETE' });
}

// ---------- 收藏夹 ----------

export function getCollections() {
  return request<CollectionVO[]>({ url: '/api/collections', method: 'GET' });
}

export function createCollection(body: { name: string; description?: string }) {
  return request<CollectionVO>({ url: '/api/collections', method: 'POST', data: body });
}

export function deleteCollection(collectionId: number) {
  return request<void>({ url: `/api/collections/${collectionId}`, method: 'DELETE' });
}

export function batchCollect(body: { collectionId: number; noteIds: number[] }) {
  return request<{ added: number }>({
    url: '/api/collections/batch',
    method: 'POST',
    data: body,
  });
}

export function getCollectionNotes(params: {
  collectionId: number;
  page?: number;
  pageSize?: number;
}) {
  return request<PageData<NoteVO>>({
    url: '/api/collection-notes',
    method: 'GET',
    params,
  });
}

export function removeFromCollection(collectionId: number, noteId: number) {
  return request<void>({
    url: `/api/collection-notes/${collectionId}/${noteId}`,
    method: 'DELETE',
  });
}

// ---------- 消息 ----------

export function getMessages(params: { type?: number; page?: number; pageSize?: number }) {
  return request<PageData<MessageVO>>({ url: '/api/messages', method: 'GET', params });
}

export function getUnreadCount() {
  return request<UnreadCount>({ url: '/api/messages/unread/count', method: 'GET' });
}

export function markMessageRead(messageId: number) {
  return request<void>({ url: `/api/messages/${messageId}/read`, method: 'PATCH' });
}

export function markAllMessagesRead() {
  return request<void>({ url: '/api/messages/all/read', method: 'PATCH' });
}

export function deleteMessage(messageId: number) {
  return request<void>({ url: `/api/messages/${messageId}`, method: 'DELETE' });
}
