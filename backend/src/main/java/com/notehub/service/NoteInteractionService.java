package com.notehub.service;

/**
 * 笔记互动: 点赞/收藏(事件驱动消息)
 */
public interface NoteInteractionService {

    /** 点赞(幂等); 返回最新点赞数 */
    int likeNote(Long noteId, Long userId);

    /** 取消点赞(幂等) */
    void unlikeNote(Long noteId, Long userId);

    /** 评论(顶层/楼中楼); 返回评论 ID */
    Long addComment(Long noteId, Long userId, Long parentId, String content);

    /** 编辑评论(仅作者) */
    void updateComment(Long commentId, Long operatorId, String content);

    /** 删除评论(软删, 仅作者); 楼中楼同时递减父评论回复数 */
    void deleteComment(Long commentId, Long operatorId);

    /** 评论点赞/取消(幂等) */
    void likeComment(Long commentId, Long userId);

    void unlikeComment(Long commentId, Long userId);
}
