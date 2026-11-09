import { describe, expect, it, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import { Provider } from 'react-redux';
import { MemoryRouter } from 'react-router-dom';
import { App as AntApp, ConfigProvider } from 'antd';
import zhCN from 'antd/locale/zh_CN';
import { configureStore } from '@reduxjs/toolkit';
import userReducer from '@/store/userSlice';
import type { UserVO } from '@/domain/user/types';
import AdminLayout from './AdminLayout';
import DashboardPage from './pages/dashboard/DashboardPage';

vi.mock('@/domain/admin/api', () => ({
  adminApi: { getStatistic: vi.fn() },
}));

import { adminApi } from '@/domain/admin/api';

const demoUser: UserVO = {
  userId: 2,
  account: 'demo',
  username: '刷题小白',
  email: null,
  gender: 0,
  birthday: null,
  avatarUrl: null,
  school: null,
  signature: null,
  isAdmin: 0,
  isBanned: 0,
  lastLoginAt: null,
  createdAt: '2026-11-01T00:00:00',
};

function renderWithState(ui: React.ReactNode, user: UserVO | null) {
  // 注意: userSlice 的 initialState 在模块加载时读取 token, 测试里需用 preloadedState 注入
  const store = configureStore({
    reducer: { user: userReducer },
    preloadedState: user
      ? { user: { token: 'token', user, loading: false } }
      : undefined,
  });
  render(
    <Provider store={store}>
      <MemoryRouter>
        <ConfigProvider locale={zhCN}>
          <AntApp>{ui}</AntApp>
        </ConfigProvider>
      </MemoryRouter>
    </Provider>,
  );
}

describe('AdminLayout 权限守卫', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    localStorage.clear();
  });

  it('普通用户 → 403 需要管理员权限', () => {
    renderWithState(<AdminLayout />, demoUser);
    // 403 副标题 + 全局提示都会出现, 断言至少一处
    expect(screen.getAllByText('需要管理员权限').length).toBeGreaterThan(0);
  });

  it('管理员 → 渲染侧边栏菜单', () => {
    renderWithState(<AdminLayout />, { ...demoUser, isAdmin: 1 });
    expect(screen.getByText('数据看板')).toBeInTheDocument();
    expect(screen.getByText('题库管理')).toBeInTheDocument();
    expect(screen.getByText('用户管理')).toBeInTheDocument();
  });
});

describe('DashboardPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(adminApi.getStatistic).mockResolvedValue({
      today: {
        loginCount: 3,
        registerCount: 1,
        totalRegisterCount: 3,
        noteCount: 2,
        submitNoteCount: 1,
        totalNoteCount: 2,
      },
      trend: [{ date: '2026-11-09', loginCount: 3, registerCount: 1, noteCount: 1 }],
    });
  });

  it('渲染今日指标与趋势表', async () => {
    renderWithState(<DashboardPage />, { ...demoUser, isAdmin: 1 });
    expect(await screen.findByText('今日登录')).toBeInTheDocument();
    expect(screen.getByText('累计笔记')).toBeInTheDocument();
    expect(await screen.findByText('2026-11-09')).toBeInTheDocument();
  });
});
