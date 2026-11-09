import { request } from '@/request/http';
import type { PageData } from '@/request/types';
import type {
  CollectionVO,
  CommentVO,
  MessageVO,
  NoteVO,
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
