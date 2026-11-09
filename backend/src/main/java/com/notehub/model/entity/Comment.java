package com.notehub.model.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 评论 (表 comments, 两级: parent_id 为空=顶层)
 */
@Data
public class Comment {

    private Long commentId;

    private Long noteId;

    private Long authorId;

    /** 父评论, 顶层为 null */
    private Long parentId;

    /** 楼中楼的目标评论(同父下定位用) */
    private Long replyToId;

    private String content;

    private Integer likeCount;

    private Integer replyCount;

    /** 软删标记: 1=已删(前端显示"已删除") */
    private Integer isDeleted;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    /** 联表附带列(users), 非表字段 */
    private String authorName;

    private String authorAvatarUrl;

    /** 联表附带列(comments 自联, 回复目标用户名), 非表字段 */
    private String replyToUserName;
}
