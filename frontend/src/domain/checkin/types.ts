/** 每日打卡状态 */
export interface CheckinStatusVO {
  today: string;
  checkedInToday: boolean;
  currentStreak: number;
  longestStreak: number;
  totalDays: number;
}
