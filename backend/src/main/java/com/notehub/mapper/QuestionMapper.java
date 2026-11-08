package com.notehub.mapper;

import com.notehub.model.dto.question.QuestionQueryParam;
import com.notehub.model.entity.Question;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface QuestionMapper {

    Question selectById(@Param("questionId") Long questionId);

    /** 分页列表(联表取分类名) */
    List<Question> listByQuery(@Param("param") QuestionQueryParam param,
                               @Param("offset") int offset, @Param("limit") int limit);

    long countByQuery(@Param("param") QuestionQueryParam param);

    int insert(Question question);

    int update(Question question);

    int deleteById(@Param("questionId") Long questionId);

    int incrementViewCount(@Param("questionId") Long questionId);

    /** 删除前置校验: 该题下的笔记数 */
    long countNotesByQuestionId(@Param("questionId") Long questionId);
}
