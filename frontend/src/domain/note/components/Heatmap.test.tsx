import { describe, expect, it } from 'vitest';
import { render, screen } from '@testing-library/react';
import Heatmap from './Heatmap';

describe('Heatmap', () => {
  it('渲染全年格子与产出标记', () => {
    render(<Heatmap year={2026} days={[{ date: '2026-01-01', count: 2 }]} />);

    expect(screen.getByLabelText('2026-01-01 2篇')).toBeInTheDocument();
    expect(screen.getByLabelText('2026-12-31 0篇')).toBeInTheDocument();
    expect(
      screen.getByRole('img', { name: '2026 年刷题热力图, 共 2 篇笔记' }),
    ).toBeInTheDocument();
  });
});
