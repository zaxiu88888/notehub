import { describe, expect, it } from 'vitest';
import { buildHeatmapGrid, heatLevel } from './heatmap';

describe('heatLevel', () => {
  it('按产出数分 5 档', () => {
    expect(heatLevel(0)).toBe(0);
    expect(heatLevel(1)).toBe(1);
    expect(heatLevel(2)).toBe(2);
    expect(heatLevel(3)).toBe(2);
    expect(heatLevel(4)).toBe(3);
    expect(heatLevel(6)).toBe(3);
    expect(heatLevel(7)).toBe(4);
  });
});

describe('buildHeatmapGrid', () => {
  const grid = buildHeatmapGrid(2026, [
    { date: '2026-01-01', count: 2 },
    { date: '2026-12-31', count: 9 },
  ]);

  it('生成 53 列 × 7 行网格', () => {
    expect(grid.weeks).toHaveLength(53);
    grid.weeks.forEach((week) => expect(week).toHaveLength(7));
  });

  it('跨年补齐格子标记 inYear=false', () => {
    expect(grid.weeks[0][0].date).toBe('2025-12-28');
    expect(grid.weeks[0][0].inYear).toBe(false);
    expect(grid.weeks[0][4].date).toBe('2026-01-01');
    expect(grid.weeks[0][4].inYear).toBe(true);
  });

  it('数据映射到正确格子', () => {
    expect(grid.weeks[0][4].count).toBe(2);
    const lastCell = grid.weeks.at(-1)!.find((cell) => cell.date === '2026-12-31')!;
    expect(lastCell.count).toBe(9);
    expect(lastCell.inYear).toBe(true);
  });

  it('月份标签从 1 月开始且不重叠', () => {
    expect(grid.monthLabels[0].label).toBe('1月');
    const indexes = grid.monthLabels.map((m) => m.weekIndex);
    indexes.slice(1).forEach((index, i) => expect(index - indexes[i]).toBeGreaterThanOrEqual(3));
  });
});
