package com.notehub.model.vo.message;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 站内消息视图
 */
@Data
public class MessageVO {

    private Long messageId;

    /** 1点赞 2评论 3系统 */
    private Integer type;

    /** 1笔记 2评论 3用户 */
    private Integer targetType;

    private Long targetId;

    private String content;

    private Integer isRead;

    private LocalDateTime createdAt;

    /** 触发者摘要(系统消息为 null) */
    private SenderVO sender;

    @Data
    public static class SenderVO {
        private Long userId;
        private String username;
        private String avatarUrl;
    }
}
