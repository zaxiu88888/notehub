package com.notehub.mapper;

import com.notehub.model.entity.CollectionNote;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CollectionNoteMapper {

    /** uk(collection,note) 兜底防重; 返回 0=已在夹内 */
    int insert(CollectionNote collectionNote);

    int delete(@Param("collectionId") Long collectionId, @Param("noteId") Long noteId);

    /** 收藏夹内的笔记 ID 列表(按收藏时间倒序) */
    List<Long> selectNoteIdsByCollection(@Param("collectionId") Long collectionId);

    /** 笔记被删除时: 从所有收藏夹清除 */
    int deleteByNoteId(@Param("noteId") Long noteId);

    /** 收藏夹删除时: 清空条目 */
    int deleteByCollectionId(@Param("collectionId") Long collectionId);

    /** 该用户的收藏夹里是否还有这篇笔记(用于收藏数递减判定) */
    long countByCreatorAndNote(@Param("creatorId") Long creatorId, @Param("noteId") Long noteId);
}
