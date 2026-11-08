package com.notehub.model.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 题目 (表 questions)
 */
@Data
public class Question {

    private Long questionId;

    private Long categoryId;

    private String title;

    /** 题干 Markdown */
    private String description;

    /** 1简单 2中等 3困难 */
    private Integer difficulty;

    /** 考点, 分号分隔 */
    private String examPoint;

    private Integer viewCount;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    /** 联表附带列(categories.name), 非表字段 */
    private String categoryName;
}
