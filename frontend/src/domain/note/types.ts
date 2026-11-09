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
