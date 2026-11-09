package com.notehub.mapper;

import com.notehub.model.dto.tag.TagNameRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface NoteTagMapper {

    int deleteByNoteId(@Param("noteId") Long noteId);

    int insertIgnore(@Param("noteId") Long noteId, @Param("tagId") Long tagId);

    List<TagNameRow> selectNamesByNoteIds(@Param("noteIds") List<Long> noteIds);
}
