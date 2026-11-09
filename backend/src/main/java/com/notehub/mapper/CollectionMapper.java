package com.notehub.mapper;

import com.notehub.model.entity.Collection;
import com.notehub.model.entity.CollectionNote;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface CollectionMapper {

    List<Collection> listByCreator(@Param("creatorId") Long creatorId);

    Collection selectById(@Param("collectionId") Long collectionId);

    /** 各收藏夹的笔记数(列表页聚合): [{collectionId, cnt}] */
    List<Map<String, Object>> countGroupByCollection(@Param("creatorId") Long creatorId);

    int insert(Collection collection);

    /** 仅创建者可删(service 校验归属) */
    int deleteById(@Param("collectionId") Long collectionId);
}
