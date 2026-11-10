/** 每日打卡状态 */
export interface CheckinStatusVO {
  today: string;
  checkedInToday: boolean;
  currentStreak: number;
  longestStreak: number;
  totalDays: number;
  /** 近 7 天轨迹(含今天) */
  recent: { date: string; checked: boolean }[];
}
