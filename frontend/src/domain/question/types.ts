/** 题库领域类型 */
export interface CategoryVO {
  categoryId: number;
  name: string;
  parentCategoryId: number | null;
  children: CategoryVO[];
}

export interface QuestionVO {
  questionId: number;
  categoryId: number;
  categoryName: string | null;
  title: string;
  description: string | null;
  difficulty: number;
  examPoint: string | null;
  viewCount: number;
  createdAt: string;
  /** 我在该题下的笔记(未登录/未写为 null) */
  myNoteId: number | null;
}

export interface QuestionListVO {
  questionListId: number;
  name: string;
  type: number;
  description: string | null;
  questionCount: number;
  createdAt: string;
}

export interface QuestionListItemVO {
  questionId: number;
  title: string;
  difficulty: number;
  examPoint: string | null;
  categoryId: number;
  categoryName: string | null;
  sortRank: number;
  hasMyNote: boolean;
}
