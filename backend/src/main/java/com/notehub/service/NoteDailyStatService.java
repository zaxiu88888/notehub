package com.notehub.service;

import com.notehub.model.vo.note.NoteDailyStatVO;

import java.time.LocalDate;
import java.util.List;

/**
 * 笔记每日互动统计: 主键去重 + 原子 upsert, 供热度榜/互动趋势使用
 */
public interface NoteDailyStatService {

    default void recordLike(Long noteId, int delta) {
        recordLike(noteId, LocalDate.now(), delta);
    }

    /** 按互动发生日归属(取消点赞/删评论/取消收藏时用原始日期) */
    void recordLike(Long noteId, LocalDate date, int delta);

    default void recordComment(Long noteId, int delta) {
        recordComment(noteId, LocalDate.now(), delta);
    }

    void recordComment(Long noteId, LocalDate date, int delta);

    default void recordCollect(Long noteId, int delta) {
        recordCollect(noteId, LocalDate.now(), delta);
    }

    void recordCollect(Long noteId, LocalDate date, int delta);

    List<NoteDailyStatVO> recent(Long noteId, Integer days);

    void deleteByNoteId(Long noteId);
}
