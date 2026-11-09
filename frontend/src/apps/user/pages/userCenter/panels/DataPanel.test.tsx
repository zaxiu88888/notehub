import { describe, expect, it, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import { App as AntApp, ConfigProvider } from 'antd';
import zhCN from 'antd/locale/zh_CN';
import DataPanel from './DataPanel';

vi.mock('@/domain/note/api', () => ({
  getNoteStats: vi.fn(),
  getHeatmap: vi.fn(),
}));

import { getHeatmap, getNoteStats } from '@/domain/note/api';

describe('DataPanel', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    const year = new Date().getFullYear();
    vi.mocked(getNoteStats).mockResolvedValue({
      noteCount: 12,
      likeCount: 34,
      commentCount: 8,
      collectCount: 5,
      wordCount: 6789,
      activeDays: 9,
      currentStreak: 3,
      longestStreak: 7,
      monthNoteCount: 4,
    });
    vi.mocked(getHeatmap).mockResolvedValue({
      year,
      total: 12,
      activeDays: 1,
      max: 2,
      days: [{ date: `${year}-01-01`, count: 2 }],
    });
  });

  it('渲染 Top3 卡片与热力图汇总', async () => {
    render(
      <ConfigProvider locale={zhCN}>
        <AntApp>
          <DataPanel />
        </AntApp>
      </ConfigProvider>,
    );

    expect(await screen.findByText('笔记总数')).toBeInTheDocument();
    expect(await screen.findByText('12')).toBeInTheDocument();
    expect(screen.getByText('收获点赞')).toBeInTheDocument();
    expect(screen.getByText('连续提交')).toBeInTheDocument();
    expect(await screen.findByText(/全年 12 篇 · 活跃 1 天 · 单日最多 2 篇/)).toBeInTheDocument();
  });
});
