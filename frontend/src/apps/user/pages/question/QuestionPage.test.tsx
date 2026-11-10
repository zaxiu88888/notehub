import { describe, expect, it, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { Provider } from 'react-redux';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { App as AntApp, ConfigProvider } from 'antd';
import zhCN from 'antd/locale/zh_CN';
import { configureStore } from '@reduxjs/toolkit';
import userReducer from '@/store/userSlice';
import QuestionPage from './QuestionPage';
import type { NoteVO } from '@/domain/note/types';
import type { QuestionVO } from '@/domain/question/types';

vi.mock('@/domain/question/api', () => ({
  getQuestion: vi.fn(),
}));
vi.mock('@/domain/note/api', () => ({
  getNotes: vi.fn(),
  getNote: vi.fn(),
  deleteNote: vi.fn(),
  downloadNotes: vi.fn(),
  likeNote: vi.fn(),
  unlikeNote: vi.fn(),
  summarizeNote: vi.fn(),
  getComments: vi.fn(),
  postComment: vi.fn(),
  editComment: vi.fn(),
  removeComment: vi.fn(),
  likeComment: vi.fn(),
  unlikeComment: vi.fn(),
  getCollections: vi.fn(),
  createCollection: vi.fn(),
  batchCollect: vi.fn(),
  removeFromCollection: vi.fn(),
}));

import { getQuestion } from '@/domain/question/api';
import { getComments, getNote, getNotes } from '@/domain/note/api';

const question: QuestionVO = {
  questionId: 3,
  categoryId: 12,
  categoryName: '链表',
  title: '反转链表',
  description: '反转单链表',
  difficulty: 1,
  examPoint: '链表;递归;迭代',
  tags: ['链表', '递归', '迭代'],
  viewCount: 3,
  createdAt: '2026-11-01T00:00:00',
  myNoteId: null,
};

const note: NoteVO = {
  noteId: 1,
  questionId: 3,
  content: '三指针迭代反转',
  likeCount: 5,
  commentCount: 0,
  collectCount: 7,
  createdAt: '2026-11-10T10:00:00',
  tags: ['链表'],
  author: { userId: 2, username: '刷题小白', avatarUrl: null },
  question: { questionId: 3, title: '反转链表', difficulty: 1 },
  myActions: { liked: false, collected: false },
};

function renderPage() {
  const store = configureStore({ reducer: { user: userReducer } });
  return render(
    <Provider store={store}>
      <MemoryRouter initialEntries={['/questions/3']}>
        <ConfigProvider locale={zhCN}>
          <AntApp>
            <Routes>
              <Route path="/questions/:questionId" element={<QuestionPage />} />
            </Routes>
          </AntApp>
        </ConfigProvider>
      </MemoryRouter>
    </Provider>,
  );
}

describe('QuestionPage', () => {
  beforeEach(() => {
    vi.mocked(getQuestion).mockResolvedValue(question);
    vi.mocked(getNotes).mockResolvedValue({ list: [note], page: 1, pageSize: 20, total: 1 });
    vi.mocked(getComments).mockResolvedValue({ list: [], page: 1, pageSize: 10, total: 0 });
  });

  it('打开评论抽屉后评论数变化会刷新当前笔记(不整页重载)', async () => {
    vi.mocked(getComments).mockResolvedValue({ list: [], page: 1, pageSize: 10, total: 1 });
    vi.mocked(getNote).mockResolvedValue({ ...note, commentCount: 1 });
    renderPage();

    const commentButton = await screen.findByRole('button', { name: /comment/i });
    expect(commentButton.textContent).toBe('0');
    commentButton.click();

    await waitFor(() => expect(getNote).toHaveBeenCalledWith(note.noteId));
    await waitFor(() =>
      expect(screen.getByRole('button', { name: /comment/i }).textContent).toBe('1'),
    );
    // 页面仍存在, 说明是局部刷新而非整页重载
    expect(screen.getByText('反转链表')).toBeInTheDocument();
    expect(getNotes).toHaveBeenCalledTimes(1);
  });
});
