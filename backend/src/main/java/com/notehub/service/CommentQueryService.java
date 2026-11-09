package com.notehub.service;

import com.notehub.common.PageVO;
import com.notehub.model.vo.comment.CommentVO;

/**
 * 评论查询服务(写操作在 NoteInteractionService)
 */
public interface CommentQueryService {

    /** 评论列表: 顶层分页 + 楼中楼批量装配(防 N+1) + 我的点赞标记 */
    PageVO<CommentVO> listComments(Long noteId, int page, int pageSize, Long currentUserId);
}
