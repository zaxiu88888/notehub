import { describe, expect, it, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import { Provider } from 'react-redux';
import { MemoryRouter } from 'react-router-dom';
import { App as AntApp, ConfigProvider } from 'antd';
import zhCN from 'antd/locale/zh_CN';
import { configureStore } from '@reduxjs/toolkit';
import userReducer from '@/store/userSlice';
import MessagePage from './MessagePage';

vi.mock('@/domain/note/api', () => ({
  getMessages: vi.fn(),
  getUnreadCount: vi.fn(),
  markMessageRead: vi.fn(),
  markAllMessagesRead: vi.fn(),
  deleteMessage: vi.fn(),
}));

import { getMessages, getUnreadCount } from '@/domain/note/api';

function renderPage() {
  const store = configureStore({ reducer: { user: userReducer } });
  return render(
    <Provider store={store}>
      <MemoryRouter>
        <ConfigProvider locale={zhCN}>
          <AntApp>
            <MessagePage />
          </AntApp>
        </ConfigProvider>
      </MemoryRouter>
    </Provider>,
  );
}

describe('MessagePage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(getUnreadCount).mockResolvedValue({ total: 3, like: 2, comment: 1, system: 0 });
    vi.mocked(getMessages).mockResolvedValue({
      page: 1,
      pageSize: 10,
      total: 2,
      list: [
        {
          messageId: 1,
          type: 1,
          targetType: 1,
          targetId: 3,
          content: '赞了你的笔记',
          isRead: 0,
          createdAt: '2026-11-09T10:00:00',
          sender: { userId: 1, username: '站长', avatarUrl: null },
        },
        {
          messageId: 2,
          type: 3,
          targetType: 3,
          targetId: 0,
          content: '欢迎来到 notehub',
          isRead: 1,
          createdAt: '2026-11-08T10:00:00',
          sender: null,
        },
      ],
    });
  });

  it('渲染消息列表与发送者/系统通知', async () => {
    renderPage();

    expect(await screen.findByText('赞了你的笔记')).toBeInTheDocument();
    expect(screen.getByText('站长')).toBeInTheDocument();
    expect(screen.getByText('欢迎来到 notehub')).toBeInTheDocument();
    expect(screen.getByText('系统通知')).toBeInTheDocument();
  });

  it('有未读时"全部已读"按钮可用', async () => {
    renderPage();
    const btn = await screen.findByRole('button', { name: '全部已读' });
    expect(btn).toBeEnabled();
  });
});
