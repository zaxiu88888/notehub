package com.notehub.model.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 标签 (表 tags, 题目/笔记共用词表)
 */
@Data
public class Tag {

    private Long tagId;

    private String name;

    private LocalDateTime createdAt;
}
