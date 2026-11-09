package com.notehub.model.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 笔记 (表 notes, 核心表)
 */
@Data
public class Note {

    private Long noteId;

    private Long authorId;

    private Long questionId;

    /** Markdown 内容 */
    private String content;

    private Integer likeCount;

    private Integer commentCount;

    private Integer collectCount;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    /** 搜索向量(题目+正文, 全文索引列, 不对外返回) */
    private String searchVector;

    /** 联表附带列(users), 非表字段 */
    private String authorName;

    private String authorAvatarUrl;

    /** 联表附带列(questions), 非表字段 */
    private String questionTitle;

    private Integer questionDifficulty;
}
