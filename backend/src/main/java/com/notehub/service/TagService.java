package com.notehub.service;

import com.notehub.model.vo.tag.TagVO;

import java.util.List;
import java.util.Map;

/**
 * 标签: 题目标签由 exam_point 同步; 笔记标签作者可选, 新建时默认继承题目
 */
public interface TagService {

    List<TagVO> popularTags(Integer limit);

    /** 批量补标签(列表防 N+1) */
    Map<Long, List<String>> tagsByQuestionIds(List<Long> questionIds);

    Map<Long, List<String>> tagsByNoteIds(List<Long> noteIds);

    List<String> questionTags(Long questionId);

    /** 用最新 examPoint 全量重建题目标签 */
    void syncQuestionTags(Long questionId, String examPoint);

    /**
     * 重建笔记标签:
     * requestedTags == null → 继承题目标签(新建默认行为);
     * 非 null(含空列表) → 以作者选择为准
     */
    void syncNoteTags(Long noteId, Long questionId, List<String> requestedTags);

    void deleteQuestionTags(Long questionId);

    void deleteNoteTags(Long noteId);
}
