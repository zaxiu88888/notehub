package com.notehub.mapper;

import com.notehub.model.entity.QuestionListItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface QuestionListItemMapper {

    /** 题单内条目(联表题目+分类, 按 sort_rank 排序) */
    List<QuestionListItem> listByQuestionListId(@Param("questionListId") Long questionListId);

    QuestionListItem selectOne(@Param("questionListId") Long questionListId,
                               @Param("questionId") Long questionId);

    int insert(QuestionListItem item);

    int delete(@Param("questionListId") Long questionListId, @Param("questionId") Long questionId);

    /** 批量排序 */
    int updateSortRank(@Param("questionListId") Long questionListId,
                       @Param("questionId") Long questionId,
                       @Param("sortRank") Integer sortRank);

    int deleteByQuestionListId(@Param("questionListId") Long questionListId);

    /** 题单内条目数 */
    long countByListId(@Param("questionListId") Long questionListId);

    /** 各题单条目数(题单列表用): [{questionListId, cnt}] */
    List<Map<String, Object>> countGroupByListId();

    /** 当前用户在指定题目集合下已有笔记的题目 ID (题单进度) */
    List<Long> selectQuestionIdsWithNote(@Param("userId") Long userId,
                                         @Param("questionIds") List<Long> questionIds);
}
