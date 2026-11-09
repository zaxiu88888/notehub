package com.notehub.model.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 收藏夹条目 (表 collection_notes, uk: collection+note)
 */
@Data
public class CollectionNote {

    private Long collectionId;

    private Long noteId;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
