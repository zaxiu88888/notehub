package com.notehub.event;

import com.notehub.model.enums.MessageTargetType;
import com.notehub.model.enums.MessageType;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 互动事件: 点赞/评论 → 消息, 事件解耦(业务方只管发布, 写消息由监听者异步完成)
 */
@Getter
public class InteractionEvent extends ApplicationEvent {

    private final Long receiverId;
    private final Long senderId;
    private final int type;        // MessageType
    private final int targetType;  // MessageTargetType
    private final Long targetId;
    private final String content;  // 摘要

    public InteractionEvent(Object source, Long receiverId, Long senderId,
                            int type, int targetType, Long targetId, String content) {
        super(source);
        this.receiverId = receiverId;
        this.senderId = senderId;
        this.type = type;
        this.targetType = targetType;
        this.targetId = targetId;
        this.content = content;
    }

    /** 点赞笔记 */
    public static InteractionEvent likeNote(Object source, Long receiverId, Long senderId, Long noteId) {
        return new InteractionEvent(source, receiverId, senderId,
                MessageType.LIKE, MessageTargetType.NOTE, noteId, "赞了你的笔记");
    }

    /** 评论笔记(顶层) */
    public static InteractionEvent commentNote(Object source, Long receiverId, Long senderId,
                                               Long noteId, String excerpt) {
        return new InteractionEvent(source, receiverId, senderId,
                MessageType.COMMENT, MessageTargetType.NOTE, noteId, excerpt);
    }

    /** 回复评论(楼中楼) */
    public static InteractionEvent replyComment(Object source, Long receiverId, Long senderId,
                                                Long noteId, String excerpt) {
        return new InteractionEvent(source, receiverId, senderId,
                MessageType.COMMENT, MessageTargetType.COMMENT, noteId, excerpt);
    }

    /** 点赞评论: 目标是评论本身(前端据此跳转定位) */
    public static InteractionEvent likeComment(Object source, Long receiverId, Long senderId,
                                               Long commentId) {
        return new InteractionEvent(source, receiverId, senderId,
                MessageType.LIKE, MessageTargetType.COMMENT, commentId, "赞了你的评论");
    }
}
