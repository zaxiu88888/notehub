package com.notehub.model.vo.questionListItem;

import lombok.Data;

/**
 * 题单条目视图: 含题目摘要 + 当前用户刷题进度(hasMyNote)
 */
@Data
public class QuestionListItemVO {

    private Long questionId;

    private String title;

    private Integer difficulty;

    private String examPoint;

    private Long categoryId;

    private String categoryName;

    private Integer sortRank;

    /** 当前登录用户在该题下是否已有笔记(未登录恒 false) */
    private boolean hasMyNote;
}
