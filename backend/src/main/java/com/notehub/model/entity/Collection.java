package com.notehub.model.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 收藏夹 (表 collections)
 */
@Data
public class Collection {

    private Long collectionId;

    private String name;

    private String description;

    private Long creatorId;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
