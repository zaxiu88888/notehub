import { describe, expect, it, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { App as AntApp, ConfigProvider } from 'antd';
import zhCN from 'antd/locale/zh_CN';
import DataPanel from './DataPanel';

vi.mock('@/domain/note/api', () => ({
  getNoteStats: vi.fn(),
  getHeatmap: vi.fn(),
}));
vi.mock('@/domain/checkin/api', () => ({
  getCheckinStatus: vi.fn(),
  checkin: vi.fn(),
}));

import { getHeatmap, getNoteStats } from '@/domain/note/api';
import { checkin, getCheckinStatus } from '@/domain/checkin/api';

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
    vi.mocked(getCheckinStatus).mockResolvedValue({
      today: `${year}-01-02`,
      checkedInToday: false,
      currentStreak: 3,
      longestStreak: 7,
      totalDays: 10,
      recent: [
        { date: `${year}-01-01`, checked: true },
        { date: `${year}-01-02`, checked: false },
      ],
    });
  });

  const renderPanel = () =>
    render(
      <ConfigProvider locale={zhCN}>
        <AntApp>
          <DataPanel />
        </AntApp>
      </ConfigProvider>,
    );

  it('渲染 Top3 卡片与热力图汇总', async () => {
    renderPanel();

    expect(await screen.findByText('每日打卡')).toBeInTheDocument();
    expect(await screen.findByText('笔记总数')).toBeInTheDocument();
    expect(await screen.findByText('12')).toBeInTheDocument();
    expect(screen.getByText('收获点赞')).toBeInTheDocument();
    expect(screen.getByText('连续提交')).toBeInTheDocument();
    expect(await screen.findByText(/全年 12 篇 · 活跃 1 天 · 单日最多 2 篇/)).toBeInTheDocument();
    expect(screen.getByText('连续打卡')).toBeInTheDocument();
    expect(screen.getByText('今日打卡')).toBeInTheDocument();
    expect(screen.getByText('近 7 天')).toBeInTheDocument();
    expect(screen.getByLabelText(`${new Date().getFullYear()}-01-01 已打卡`)).toBeInTheDocument();
  });

  it('点击打卡: 调用接口并切换为已打卡', async () => {
    vi.mocked(checkin).mockResolvedValue({
      today: '2026-01-02',
      checkedInToday: true,
      currentStreak: 4,
      longestStreak: 7,
      totalDays: 11,
      recent: [{ date: '2026-01-02', checked: true }],
    });
    const user = userEvent.setup();
    renderPanel();

    await user.click(await screen.findByRole('button', { name: /今日打卡/ }));

    expect(checkin).toHaveBeenCalledTimes(1);
    expect(await screen.findByText('今日已打卡')).toBeInTheDocument();
  });
});
