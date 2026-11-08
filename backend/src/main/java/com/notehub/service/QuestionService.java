package com.notehub.service;

import com.notehub.common.PageVO;
import com.notehub.model.dto.question.CreateQuestionBatchBody;
import com.notehub.model.dto.question.QuestionQueryParam;
import com.notehub.model.vo.question.BatchImportResultVO;
import com.notehub.model.vo.question.QuestionVO;

/**
 * 题目服务
 */
public interface QuestionService {

    PageVO<QuestionVO> listQuestions(QuestionQueryParam param);

    /** 详情: 浏览数 +1; 登录时附带 myNoteId(当前用户在该题下的笔记, M3 前恒 null) */
    QuestionVO getQuestionDetail(Long questionId, Long currentUserId);

    QuestionVO create(Long categoryId, String title, Integer difficulty, String examPoint, String description);

    QuestionVO update(Long questionId, Long categoryId, String title, Integer difficulty,
                      String examPoint, String description);

    /** 该题下有笔记时拒绝删除 */
    void delete(Long questionId);

    /** Markdown 批量导入: 逐题校验, 失败明细返回 */
    BatchImportResultVO batchImport(CreateQuestionBatchBody body);
}
