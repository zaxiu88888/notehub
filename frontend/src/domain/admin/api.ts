import { request } from '@/request/http';
import type { PageData } from '@/request/types';
import type { CategoryVO, QuestionListVO, QuestionVO } from '@/domain/question/types';
import type { UserVO } from '@/domain/user/types';
import type { QuestionListItemVO } from '@/domain/question/types';
import type { StatisticVO } from '@/domain/statistic/types';

/** 管理端接口集合(全部需要 admin token) */
export const adminApi = {
  // ---------- 统计 ----------
  getStatistic(days = 30) {
    return request<StatisticVO>({ url: '/api/statistic', method: 'GET', params: { days } });
  },

  // ---------- 题目 ----------
  listQuestions(params: { categoryId?: number; difficulty?: number; page?: number; pageSize?: number }) {
    return request<PageData<QuestionVO>>({ url: '/api/questions', method: 'GET', params });
  },
  createQuestion(body: {
    categoryId: number;
    title: string;
    difficulty: number;
    examPoint?: string;
    description?: string;
  }) {
    return request<QuestionVO>({ url: '/api/admin/questions', method: 'POST', data: body });
  },
  updateQuestion(
    questionId: number,
    body: { categoryId?: number; title?: string; difficulty?: number; examPoint?: string; description?: string },
  ) {
    return request<QuestionVO>({
      url: `/api/admin/questions/${questionId}`,
      method: 'PATCH',
      data: body,
    });
  },
  deleteQuestion(questionId: number) {
    return request<void>({ url: `/api/admin/questions/${questionId}`, method: 'DELETE' });
  },
  batchImportQuestions(body: { categoryId: number; markdown: string }) {
    return request<{ total: number; success: number; failures: { title: string; reason: string }[] }>({
      url: '/api/admin/questions/batch',
      method: 'POST',
      data: body,
    });
  },

  // ---------- 分类 ----------
  createCategory(body: { name: string; parentCategoryId?: number }) {
    return request<CategoryVO>({ url: '/api/admin/categories', method: 'POST', data: body });
  },
  updateCategory(categoryId: number, body: { name?: string; parentCategoryId?: number }) {
    return request<CategoryVO>({
      url: `/api/admin/categories/${categoryId}`,
      method: 'PATCH',
      data: body,
    });
  },
  deleteCategory(categoryId: number) {
    return request<void>({ url: `/api/admin/categories/${categoryId}`, method: 'DELETE' });
  },

  // ---------- 题单 ----------
  listQuestionLists() {
    return request<QuestionListVO[]>({ url: '/api/questionlists', method: 'GET' });
  },
  createQuestionList(body: { name: string; type: number; description?: string }) {
    return request<QuestionListVO>({ url: '/api/admin/questionlists', method: 'POST', data: body });
  },
  updateQuestionList(
    questionListId: number,
    body: { name?: string; type?: number; description?: string },
  ) {
    return request<QuestionListVO>({
      url: `/api/admin/questionlists/${questionListId}`,
      method: 'PATCH',
      data: body,
    });
  },
  deleteQuestionList(questionListId: number) {
    return request<void>({ url: `/api/admin/questionlists/${questionListId}`, method: 'DELETE' });
  },
  listQuestionListItems(questionListId: number) {
    return request<QuestionListItemVO[]>({
      url: `/api/admin/questionlist-items/${questionListId}`,
      method: 'GET',
    });
  },
  addQuestionListItem(body: { questionListId: number; questionId: number }) {
    return request<void>({ url: '/api/admin/questionlist-items', method: 'POST', data: body });
  },
  removeQuestionListItem(questionListId: number, questionId: number) {
    return request<void>({
      url: `/api/admin/questionlist-items/${questionListId}/${questionId}`,
      method: 'DELETE',
    });
  },
  sortQuestionListItems(questionListId: number, questionIds: number[]) {
    return request<void>({
      url: '/api/admin/questionlist-items/sort',
      method: 'PATCH',
      data: { questionListId, items: questionIds.map((questionId) => ({ questionId })) },
    });
  },

  // ---------- 用户 ----------
  listUsers(params: { account?: string; banned?: number; page?: number; pageSize?: number }) {
    return request<PageData<UserVO>>({ url: '/api/admin/users', method: 'GET', params });
  },
  banUser(userId: number, isBanned: 0 | 1) {
    return request<void>({
      url: `/api/admin/users/${userId}/ban`,
      method: 'PATCH',
      data: { isBanned },
    });
  },
};
