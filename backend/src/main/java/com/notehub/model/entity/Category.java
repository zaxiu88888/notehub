package com.notehub.model.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 题目分类 (表 categories, 两级)
 */
@Data
public class Category {

    private Long categoryId;

    private String name;

    /** 父分类 ID, 顶级为 null */
    private Long parentCategoryId;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
