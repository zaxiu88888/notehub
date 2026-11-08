package com.notehub.model.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 题单 (表 question_lists)
 */
@Data
public class QuestionList {

    private Long questionListId;

    private String name;

    /** 1官方推荐 2专题 */
    private Integer type;

    private String description;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
