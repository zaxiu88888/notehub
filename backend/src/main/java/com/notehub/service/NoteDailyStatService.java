package com.notehub.service;

import com.notehub.model.vo.note.NoteDailyStatVO;

import java.util.List;

/**
 * 笔记每日互动统计: 主键去重 + 原子 upsert, 供热度榜/互动趋势使用
 */
public interface NoteDailyStatService {

    void recordLike(Long noteId, int delta);

    void recordComment(Long noteId, int delta);

    void recordCollect(Long noteId, int delta);

    List<NoteDailyStatVO> recent(Long noteId, Integer days);

    void deleteByNoteId(Long noteId);
}
