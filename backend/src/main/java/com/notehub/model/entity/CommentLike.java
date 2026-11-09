package com.notehub.model.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 评论点赞 (表 comment_likes, uk: comment+user)
 */
@Data
public class CommentLike {

    private Long commentId;

    private Long userId;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
