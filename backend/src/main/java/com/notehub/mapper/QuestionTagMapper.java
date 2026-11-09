package com.notehub.mapper;

import com.notehub.model.dto.tag.TagNameRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface QuestionTagMapper {

    int deleteByQuestionId(@Param("questionId") Long questionId);

    int insertIgnore(@Param("questionId") Long questionId, @Param("tagId") Long tagId);

    List<String> selectNamesByQuestionId(@Param("questionId") Long questionId);

    List<TagNameRow> selectNamesByQuestionIds(@Param("questionIds") List<Long> questionIds);
}
