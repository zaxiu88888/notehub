package com.notehub.service;

import com.notehub.model.vo.note.HeatmapVO;
import com.notehub.model.vo.note.NoteStatsVO;
import com.notehub.model.vo.note.RankNoteVO;

import java.time.LocalDate;
import java.util.List;

/**
 * I2 数据可视化: 热力图 / 热度榜 / 个人数据卡片 / 笔记导出
 */
public interface NoteAnalyticsService {

    /** 刷题热力图(Redis 位图缓存, 失效从库回填) */
    HeatmapVO getHeatmap(Long userId, Integer year);

    /** 笔记热度榜 period=week|month|all */
    List<RankNoteVO> getRanklist(String period, Integer limit);

    /** 个人数据卡片 */
    NoteStatsVO getTopStats(Long userId);

    /** 导出笔记: noteId 为空导出全部, 否则单篇(仅作者) */
    byte[] exportNotes(Long userId, Long noteId);

    /** 笔记增删后维护热力图位图; Redis 异常不影响主流程 */
    void recordHeatmap(Long userId, LocalDate day, int delta);
}
