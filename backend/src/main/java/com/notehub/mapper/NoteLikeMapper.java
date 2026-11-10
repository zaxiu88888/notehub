package com.notehub.mapper;

import com.notehub.model.entity.NoteLike;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface NoteLikeMapper {

    /** uk(note,user) 兜底防重; 返回 0=已赞过 */
    int insert(NoteLike noteLike);

    /** 取消前取原始点赞时间(每日统计按发生日回退) */
    NoteLike select(@Param("noteId") Long noteId, @Param("userId") Long userId);

    /** 返回 0=原本就没赞过(幂等取消) */
    int delete(@Param("noteId") Long noteId, @Param("userId") Long userId);

    int countByNote(@Param("noteId") Long noteId);
}
