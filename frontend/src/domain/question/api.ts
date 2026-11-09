import { request } from '@/request/http';
import type { PageData } from '@/request/types';
import type {
  CategoryVO,
  QuestionListItemVO,
  QuestionListVO,
  QuestionVO,
} from './types';

export function getCategoryTree() {
  return request<CategoryVO[]>({ url: '/api/categories', method: 'GET' });
}

export function getQuestions(params: {
  categoryId?: number;
  difficulty?: number;
  tag?: string;
  page?: number;
  pageSize?: number;
}) {
  return request<PageData<QuestionVO>>({ url: '/api/questions', method: 'GET', params });
}

export function getQuestion(questionId: number) {
  return request<QuestionVO>({ url: `/api/questions/${questionId}`, method: 'GET' });
}

export function getQuestionLists() {
  return request<QuestionListVO[]>({ url: '/api/questionlists', method: 'GET' });
}

export function getQuestionListItems(questionListId: number) {
  return request<QuestionListItemVO[]>({
    url: '/api/questionlist-items',
    method: 'GET',
    params: { questionListId },
  });
}
