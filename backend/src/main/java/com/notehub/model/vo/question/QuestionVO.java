package com.notehub.model.vo.question;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 题目视图: 列表不带 description; 详情带 description + myNote
 */
@Data
public class QuestionVO {

    private Long questionId;

    private Long categoryId;

    private String categoryName;

    private String title;

    private String description;

    private Integer difficulty;

    private String examPoint;

    /** 由 exam_point 同步的标签(无标签为空列表) */
    private List<String> tags;

    private Integer viewCount;

    private LocalDateTime createdAt;

    /** 当前登录用户在该题下的笔记 ID(未登录/无笔记为 null; 笔记模块 M3 完整接入) */
    private Long myNoteId;
}
