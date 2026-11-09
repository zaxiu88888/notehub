package com.notehub.model.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 笔记点赞 (表 note_likes, uk: note+user)
 */
@Data
public class NoteLike {

    private Long noteId;

    private Long userId;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
