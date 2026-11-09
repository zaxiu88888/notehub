import { describe, expect, it, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Provider } from 'react-redux';
import { MemoryRouter } from 'react-router-dom';
import { App as AntApp, ConfigProvider } from 'antd';
import zhCN from 'antd/locale/zh_CN';
import { configureStore } from '@reduxjs/toolkit';
import userReducer from '@/store/userSlice';
import SearchPage from './SearchPage';

vi.mock('@/domain/search/api', () => ({
  searchNotes: vi.fn(),
  searchUsers: vi.fn(),
}));

import { searchNotes, searchUsers } from '@/domain/search/api';

function renderSearch(initialEntry: string) {
  const store = configureStore({ reducer: { user: userReducer } });
  return render(
    <Provider store={store}>
      <MemoryRouter initialEntries={[initialEntry]}>
        <ConfigProvider locale={zhCN}>
          <AntApp>
            <SearchPage />
          </AntApp>
        </ConfigProvider>
      </MemoryRouter>
    </Provider>,
  );
}

describe('SearchPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(searchNotes).mockResolvedValue({
      page: 1,
      pageSize: 10,
      total: 1,
      list: [
        {
          noteId: 1,
          questionId: 2,
          content: '# 两数之和题解\n哈希表一次遍历',
          likeCount: 1,
          commentCount: 0,
          collectCount: 0,
          createdAt: '2026-11-09T10:00:00',
          author: { userId: 2, username: '刷题小白', avatarUrl: null },
          question: { questionId: 2, title: '两数之和', difficulty: 1 },
          myActions: { liked: false, collected: false },
        },
      ],
    });
    vi.mocked(searchUsers).mockResolvedValue({
      page: 1,
      pageSize: 10,
      total: 1,
      list: [
        {
          userId: 2,
          account: 'demo',
          username: '刷题小白',
          email: null,
          gender: 0,
          birthday: null,
          avatarUrl: null,
          school: null,
          signature: '每天进步一点点',
          isAdmin: 0,
          isBanned: 0,
          lastLoginAt: null,
          createdAt: '2026-11-01T00:00:00',
        },
      ],
    });
  });

  it('按关键词展示笔记结果', async () => {
    renderSearch('/search?keyword=哈希表');

    expect(await screen.findByText(/哈希表一次遍历/)).toBeInTheDocument();
    expect(screen.getByText('搜索「哈希表」')).toBeInTheDocument();
    expect(searchNotes).toHaveBeenCalledWith(
      expect.objectContaining({ keyword: '哈希表', page: 1 }),
    );
  });

  it('切换用户页签展示用户结果', async () => {
    const user = userEvent.setup();
    renderSearch('/search?keyword=demo');

    await user.click(screen.getByRole('tab', { name: /用户/ }));

    // 笔记页签仍保留在 DOM(antd Tabs), 作者名可能重复 → 用签名与链接断言
    expect(await screen.findByText('每天进步一点点')).toBeInTheDocument();
    expect(screen.getAllByText('刷题小白').length).toBeGreaterThan(0);
  });

  it('无关键词: 展示引导空态, 不发请求', () => {
    renderSearch('/search');
    expect(screen.getByText(/在顶部搜索框输入关键词/)).toBeInTheDocument();
    expect(searchNotes).not.toHaveBeenCalled();
  });
});
