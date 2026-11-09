package com.notehub.model.vo.collection;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 收藏夹视图
 */
@Data
public class CollectionVO {

    private Long collectionId;

    private String name;

    private String description;

    private long noteCount;

    private LocalDateTime createdAt;
}
