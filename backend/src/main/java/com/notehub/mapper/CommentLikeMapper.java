package com.notehub.mapper;

import com.notehub.model.entity.CommentLike;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface CommentLikeMapper {

    /** uk(comment,user) 兜底防重; 返回 0=已赞过 */
    int insert(CommentLike commentLike);

    /** 返回 0=原本没赞过(幂等取消) */
    int delete(@Param("commentId") Long commentId, @Param("userId") Long userId);
}
