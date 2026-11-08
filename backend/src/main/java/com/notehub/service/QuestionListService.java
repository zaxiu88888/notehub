package com.notehub.service;

import com.notehub.model.vo.questionList.QuestionListVO;
import com.notehub.model.vo.questionListItem.QuestionListItemVO;

import java.util.List;

/**
 * 题单与题单条目服务
 */
public interface QuestionListService {

    List<QuestionListVO> listQuestionLists();

    QuestionListVO getQuestionList(Long questionListId);

    QuestionListVO create(String name, Integer type, String description);

    QuestionListVO update(Long questionListId, String name, Integer type, String description);

    /** 删除题单(连带条目) */
    void delete(Long questionListId);

    /** 题单条目: 排序返回, 登录时标记 hasMyNote(刷题进度) */
    List<QuestionListItemVO> listItems(Long questionListId, Long currentUserId);

    /** 管理端查看条目(不做我的笔记标记) */
    List<QuestionListItemVO> listItemsForAdmin(Long questionListId);

    void addItem(Long questionListId, Long questionId, Integer sortRank);

    void removeItem(Long questionListId, Long questionId);

    /** 批量排序: items 顺序即 1..n */
    void sortItems(Long questionListId, List<Long> questionIdsInOrder);
}
