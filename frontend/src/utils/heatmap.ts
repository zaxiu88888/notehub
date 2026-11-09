import dayjs from 'dayjs';

/** 热力图 5 档配色(GitHub 风格) */
export const HEATMAP_COLORS = ['#ebedf0', '#9be9a8', '#40c463', '#30a14e', '#216e39'];

export interface HeatmapCell {
  date: string;
  count: number;
  /** 是否属于目标年份(跨年补齐的空格子为 false) */
  inYear: boolean;
}

export interface HeatmapMonthLabel {
  label: string;
  weekIndex: number;
}

export interface HeatmapGrid {
  weeks: HeatmapCell[][];
  monthLabels: HeatmapMonthLabel[];
}

/** 计数 → 色阶: 0 / 1 / 2-3 / 4-6 / 7+ */
export function heatLevel(count: number): number {
  if (count <= 0) return 0;
  if (count === 1) return 1;
  if (count <= 3) return 2;
  if (count <= 6) return 3;
  return 4;
}

/**
 * 构建一年热力图网格: 周日为一列起点, 每列 7 天;
 * 月份标签仅在距上一个标签 >= 3 周时展示, 避免挤压
 */
export function buildHeatmapGrid(
  year: number,
  days: { date: string; count: number }[],
): HeatmapGrid {
  const countByDate = new Map(days.map((d) => [d.date, d.count]));
  const yearStart = dayjs(`${year}-01-01`);
  const yearEnd = dayjs(`${year}-12-31`);
  const gridStart = yearStart.subtract(yearStart.day(), 'day');

  const weeks: HeatmapCell[][] = [];
  let week: HeatmapCell[] = [];
  let cursor = gridStart;

  while (cursor.isBefore(yearEnd) || cursor.isSame(yearEnd, 'day')) {
    const date = cursor.format('YYYY-MM-DD');
    week.push({
      date,
      count: countByDate.get(date) ?? 0,
      inYear: cursor.year() === year,
    });
    if (week.length === 7) {
      weeks.push(week);
      week = [];
    }
    cursor = cursor.add(1, 'day');
  }

  if (week.length > 0) {
    while (week.length < 7) {
      week.push({ date: '', count: 0, inYear: false });
    }
    weeks.push(week);
  }

  const monthLabels: HeatmapMonthLabel[] = [];
  let lastWeek = -10;
  for (let month = 1; month <= 12; month++) {
    const firstDay = dayjs(`${year}-${String(month).padStart(2, '0')}-01`);
    const weekIndex = Math.floor(firstDay.diff(gridStart, 'day') / 7);
    if (weekIndex - lastWeek >= 3) {
      monthLabels.push({ label: `${month}月`, weekIndex });
      lastWeek = weekIndex;
    }
  }

  return { weeks, monthLabels };
}
