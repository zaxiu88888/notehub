/** 笔记 / 互动 领域类型 */
export interface NoteAuthor {
  userId: number;
  username: string;
  avatarUrl: string | null;
}

export interface NoteQuestionBrief {
  questionId: number;
  title: string;
  difficulty: number;
}

export interface MyActions {
  liked: boolean;
  collected: boolean;
}

export interface NoteVO {
  noteId: number;
  questionId: number;
  content: string;
  likeCount: number;
  commentCount: number;
  collectCount: number;
  createdAt: string;
  /** 标签名(无标签为空数组) */
  tags: string[];
  author: NoteAuthor;
  question: NoteQuestionBrief;
  myActions: MyActions;
}

export interface CommentVO {
  commentId: number;
  noteId: number;
  parentId: number | null;
  replyToId: number | null;
  content: string;
  likeCount: number;
  replyCount: number;
  isDeleted: number;
  createdAt: string;
  author: NoteAuthor;
  replyToUserName: string | null;
  liked: boolean;
  replies: CommentVO[];
}

export interface CollectionVO {
  collectionId: number;
  name: string;
  description: string | null;
  noteCount: number;
  createdAt: string;
}

export interface MessageVO {
  messageId: number;
  type: number;
  targetType: number;
  targetId: number;
  content: string;
  isRead: number;
  createdAt: string;
  sender: NoteAuthor | null;
}

export interface UnreadCount {
  total: number;
  like: number;
  comment: number;
  system: number;
}

// ---------- I2 数据可视化 ----------

export interface HeatmapDay {
  date: string;
  count: number;
}

export interface HeatmapVO {
  year: number;
  total: number;
  activeDays: number;
  max: number;
  days: HeatmapDay[];
}

export type RankPeriod = 'week' | 'month' | 'all';

export interface RankNoteVO {
  rank: number;
  noteId: number;
  questionId: number;
  questionTitle: string;
  difficulty: number;
  author: NoteAuthor;
  excerpt: string;
  likeCount: number;
  commentCount: number;
  collectCount: number;
  hotScore: number;
}

export interface AiSummaryVO {
  noteId: number;
  summary: string;
  /** ai=模型生成 local=未配置/失败时的本地摘要 */
  source: 'ai' | 'local';
}

export interface NoteStatsVO {
  noteCount: number;
  likeCount: number;
  commentCount: number;
  collectCount: number;
  wordCount: number;
  activeDays: number;
  currentStreak: number;
  longestStreak: number;
  monthNoteCount: number;
}
