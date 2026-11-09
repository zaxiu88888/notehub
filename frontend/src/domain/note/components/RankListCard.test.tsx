import { describe, expect, it, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { App as AntApp, ConfigProvider } from 'antd';
import zhCN from 'antd/locale/zh_CN';
import RankListCard from './RankListCard';

vi.mock('@/domain/note/api', () => ({
  getNoteRanklist: vi.fn(),
}));

import { getNoteRanklist } from '@/domain/note/api';

describe('RankListCard', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(getNoteRanklist).mockResolvedValue([
      {
        rank: 1,
        noteId: 1,
        questionId: 2,
        questionTitle: '两数之和',
        difficulty: 1,
        author: { userId: 2, username: '刷题小白', avatarUrl: null },
        excerpt: '哈希表一次遍历',
        likeCount: 3,
        commentCount: 2,
        collectCount: 1,
        hotScore: 14,
      },
    ]);
  });

  it('渲染榜单条目与题目链接', async () => {
    render(
      <MemoryRouter>
        <ConfigProvider locale={zhCN}>
          <AntApp>
            <RankListCard />
          </AntApp>
        </ConfigProvider>
      </MemoryRouter>,
    );

    expect(await screen.findByText('两数之和')).toBeInTheDocument();
    expect(screen.getByText('刷题小白')).toBeInTheDocument();
    expect(screen.getByText('哈希表一次遍历')).toBeInTheDocument();
    expect(screen.getByText('14')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: '两数之和' })).toHaveAttribute(
      'href',
      '/questions/2',
    );
    expect(getNoteRanklist).toHaveBeenCalledWith({ period: 'week', limit: 10 });
  });
});
