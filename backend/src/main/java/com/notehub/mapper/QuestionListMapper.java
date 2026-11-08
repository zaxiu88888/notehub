package com.notehub.mapper;

import com.notehub.model.entity.QuestionList;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface QuestionListMapper {

    List<QuestionList> selectAll();

    QuestionList selectById(Long questionListId);

    int insert(QuestionList questionList);

    int update(QuestionList questionList);

    int deleteById(Long questionListId);
}
