package com.notehub.model.vo.questionList;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 题单视图
 */
@Data
public class QuestionListVO {

    private Long questionListId;

    private String name;

    private Integer type;

    private String description;

    /** 题单内题目数 */
    private long questionCount;

    private LocalDateTime createdAt;
}
