package com.notehub.mapper;

import com.notehub.model.entity.Tag;
import com.notehub.model.vo.tag.TagVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TagMapper {

    /** 名称不存在则插入, 存在则回填 tagId(LAST_INSERT_ID 技巧) */
    int upsert(Tag tag);

    Tag selectByName(@Param("name") String name);

    /** 热门标签: 按题目/笔记关联总数倒序 */
    List<TagVO> selectPopular(@Param("limit") int limit);
}
