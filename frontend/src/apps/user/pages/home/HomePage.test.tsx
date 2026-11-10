import { describe, expect, it, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Provider } from 'react-redux';
import { MemoryRouter } from 'react-router-dom';
import { App as AntApp, ConfigProvider } from 'antd';
import zhCN from 'antd/locale/zh_CN';
import { configureStore } from '@reduxjs/toolkit';
import userReducer from '@/store/userSlice';
import HomePage from './HomePage';
import type { NoteVO } from '@/domain/note/types';
import type { QuestionVO } from '@/domain/question/types';

// 领域 API 全部 mock: 组件测试只关心渲染与分支
vi.mock('@/domain/question/api', () => ({
  getCategoryTree: vi.fn(),
  getQuestions: vi.fn(),
}));
vi.mock('@/domain/note/api', () => ({
  getNotes: vi.fn(),
  getNoteRanklist: vi.fn(),
}));
vi.mock('@/domain/tag/api', () => ({
  getTags: vi.fn(),
}));
vi.mock('@/domain/note/components/CommentsPanel', async () => {
  const React = await import('react');
  return {
    default: ({ noteId }: { noteId: number }) =>
      React.createElement('div', { 'data-testid': 'comments-panel' }, `评论面板 ${noteId}`),
  };
});

import { getCategoryTree, getQuestions } from '@/domain/question/api';
import { getNoteRanklist, getNotes } from '@/domain/note/api';
import { getTags } from '@/domain/tag/api';

const question: QuestionVO = {
  questionId: 2,
  categoryId: 11,
  categoryName: '哈希表',
  title: '两数之和',
  description: null,
  difficulty: 1,
  examPoint: '数组;哈希表',
  tags: ['数组', '哈希表'],
  viewCount: 12,
  createdAt: '2026-11-01T00:00:00',
  myNoteId: null,
};

const note: NoteVO = {
  noteId: 1,
  questionId: 2,
  content: '哈希表一次遍历即可',
  likeCount: 3,
  commentCount: 1,
  collectCount: 0,
  createdAt: '2026-11-09T10:00:00',
  tags: ['哈希表'],
  author: { userId: 2, username: '刷题小白', avatarUrl: null },
  question: { questionId: 2, title: '两数之和', difficulty: 1 },
  myActions: { liked: false, collected: false },
};

function renderHome(initialEntry = '/') {
  const store = configureStore({ reducer: { user: userReducer } });
  return render(
    <Provider store={store}>
      <MemoryRouter initialEntries={[initialEntry]}>
        <ConfigProvider locale={zhCN}>
          <AntApp>
            <HomePage />
          </AntApp>
        </ConfigProvider>
      </MemoryRouter>
    </Provider>,
  );
}

describe('HomePage', () => {
  beforeEach(() => {
    vi.mocked(getCategoryTree).mockResolvedValue([
      {
        categoryId: 1,
        name: '数据结构与算法',
        parentCategoryId: null,
        children: [
          { categoryId: 11, name: '哈希表', parentCategoryId: 1, children: [] },
        ],
      },
    ]);
    vi.mocked(getQuestions).mockResolvedValue({ list: [question], page: 1, pageSize: 10, total: 1 });
    vi.mocked(getNotes).mockResolvedValue({ list: [note], page: 1, pageSize: 8, total: 1 });
    vi.mocked(getNoteRanklist).mockResolvedValue([]);
    vi.mocked(getTags).mockResolvedValue([
      { tagId: 1, name: '数组', questionCount: 2, noteCount: 0 },
      { tagId: 2, name: '哈希表', questionCount: 2, noteCount: 1 },
    ]);
  });

  it('渲染题目列表与最新笔记', async () => {
    renderHome();

    expect(await screen.findByText('两数之和')).toBeInTheDocument();
    expect(await screen.findByText('刷题小白')).toBeInTheDocument();
    expect(screen.getByText('哈希表一次遍历即可')).toBeInTheDocument();
    expect(screen.getByText('12 次浏览')).toBeInTheDocument();
  });

  it('题目为空时展示空态', async () => {
    vi.mocked(getQuestions).mockResolvedValue({ list: [], page: 1, pageSize: 10, total: 0 });
    renderHome();

    expect(await screen.findByText('该筛选条件下暂无题目')).toBeInTheDocument();
  });

  it('URL ?tag= 初始化题目标签筛选', async () => {
    renderHome('/?tag=哈希表');

    await waitFor(() =>
      expect(getQuestions).toHaveBeenCalledWith(expect.objectContaining({ tag: '哈希表' })),
    );
  });

  it('点击笔记评论数打开评论抽屉', async () => {
    const user = userEvent.setup();
    renderHome();

    await user.click(await screen.findByRole('button', { name: /comment/i }));

    expect(await screen.findByTestId('comments-panel')).toHaveTextContent('评论面板 1');
  });
});
