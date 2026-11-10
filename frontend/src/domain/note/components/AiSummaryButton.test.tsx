import { describe, expect, it, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Provider } from 'react-redux';
import { App as AntApp } from 'antd';
import { configureStore } from '@reduxjs/toolkit';
import userReducer from '@/store/userSlice';
import AiSummaryButton from './AiSummaryButton';

vi.mock('@/domain/note/api', () => ({
  summarizeNote: vi.fn(),
}));
vi.mock('@/utils/auth', () => ({
  requireLogin: () => true,
}));

import { summarizeNote } from '@/domain/note/api';

describe('AiSummaryButton', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(summarizeNote).mockResolvedValue({
      noteId: 1,
      summary: '1. 哈希表一次遍历\n2. 时间复杂度 O(n)',
      source: 'ai',
    });
  });

  function renderButton() {
    const store = configureStore({ reducer: { user: userReducer } });
    render(
      <Provider store={store}>
        <AntApp>
          <AiSummaryButton noteId={1} />
        </AntApp>
      </Provider>,
    );
  }

  it('点击生成并展示 AI 摘要, 再次打开不重复请求', async () => {
    const user = userEvent.setup();
    renderButton();

    await user.click(screen.getByRole('button', { name: 'AI 总结' }));

    expect(await screen.findByText(/哈希表一次遍历/)).toBeInTheDocument();
    expect(screen.getByText('AI 生成')).toBeInTheDocument();
    expect(summarizeNote).toHaveBeenCalledTimes(1);

    // 关闭后再次打开: 使用组件内已有结果
    await user.click(screen.getByRole('button', { name: 'Close' }));
    await user.click(screen.getByRole('button', { name: 'AI 总结' }));
    await waitFor(() => expect(summarizeNote).toHaveBeenCalledTimes(1));
  });
});
