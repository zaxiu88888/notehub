package com.notehub.service;

import com.notehub.model.vo.note.AiSummaryVO;

/**
 * AI 题解总结: 优先模型, 未配置/失败时用本地摘要; 结果缓存并随笔记编辑失效
 */
public interface AiSummaryService {

    AiSummaryVO summarize(Long noteId, Long userId);

    void evict(Long noteId);
}
