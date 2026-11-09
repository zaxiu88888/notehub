package com.notehub.mapper;

import com.notehub.model.entity.Comment;
import com.notehub.model.entity.CommentLike;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CommentMapper {

    /** 顶层评论(分页, 联表作者) */
    List<Comment> selectTopLevel(@Param("noteId") Long noteId,
                                 @Param("offset") int offset, @Param("limit") int limit);

    long countTopLevel(@Param("noteId") Long noteId);

    /** 批量取一组顶层评论的楼中楼回复(联表作者+回复目标) */
    List<Comment> selectRepliesByParentIds(@Param("parentIds") List<Long> parentIds);

    Comment selectById(@Param("commentId") Long commentId);

    int insert(Comment comment);

    /** 仅作者本人且未软删 */
    int updateContent(@Param("commentId") Long commentId,
                      @Param("authorId") Long authorId,
                      @Param("content") String content);

    /** 软删(仅作者); 回复数在 service 层对父评论增减 */
    int softDelete(@Param("commentId") Long commentId, @Param("authorId") Long authorId);

    /** 原子回复计数 */
    int adjustReplyCount(@Param("commentId") Long commentId, @Param("delta") int delta);

    /** 原子点赞计数 */
    int adjustLikeCount(@Param("commentId") Long commentId, @Param("delta") int delta);

    /** 笔记删除时: 批量软删其下所有评论 */
    int softDeleteByNoteId(@Param("noteId") Long noteId);

    /** 批量取用户点赞过的评论 ID */
    List<Long> selectLikedCommentIds(@Param("userId") Long userId,
                                     @Param("commentIds") List<Long> commentIds);
}
