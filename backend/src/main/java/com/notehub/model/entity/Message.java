package com.notehub.model.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 站内消息 (表 messages)
 */
@Data
public class Message {

    private Long messageId;

    private Long receiverId;

    /** 触发者, 系统消息=0 */
    private Long senderId;

    /** 1点赞 2评论 3系统 (MessageType) */
    private Integer type;

    /** 1笔记 2评论 3用户 (MessageTargetType) */
    private Integer targetType;

    private Long targetId;

    /** 摘要内容 */
    private String content;

    private Integer isRead;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    /** 联表附带列(users sender), 非表字段 */
    private String senderName;

    private String senderAvatarUrl;
}
