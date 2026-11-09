/** 统计看板类型 */
export interface StatisticVO {
  today: {
    loginCount: number;
    registerCount: number;
    totalRegisterCount: number;
    noteCount: number;
    submitNoteCount: number;
    totalNoteCount: number;
  };
  trend: Array<{
    date: string;
    loginCount: number;
    registerCount: number;
    noteCount: number;
  }>;
}
