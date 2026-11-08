package com.notehub.model.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 题单条目 (表 question_list_items, uk: 题单+题目)
 */
@Data
public class QuestionListItem {

    private Long questionListId;

    private Long questionId;

    /** 排序值, 越小越靠前 (rank 是 MySQL8 保留字, 故用 sort_rank) */
    private Integer sortRank;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    // ---- 联表附带列(非本表字段), 用于题单条目展示 ----
    private String title;

    private Integer difficulty;

    private String examPoint;

    private Integer viewCount;

    private Long categoryId;

    private String categoryName;
}
