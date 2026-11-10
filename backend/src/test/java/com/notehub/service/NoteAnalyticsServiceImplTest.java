package com.notehub.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.notehub.common.BusinessException;
import com.notehub.common.ErrorCode;
import com.notehub.mapper.NoteMapper;
import com.notehub.model.dto.note.DailyCountRow;
import com.notehub.model.dto.note.NoteTotalsRow;
import com.notehub.model.entity.Note;
import com.notehub.model.enums.RedisKey;
import com.notehub.model.vo.note.HeatmapVO;
import com.notehub.model.vo.note.NoteStatsVO;
import com.notehub.model.vo.note.RankNoteVO;
import com.notehub.service.impl.NoteAnalyticsServiceImpl;
import com.notehub.utils.StreakUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NoteAnalyticsServiceImplTest {

    private NoteMapper noteMapper;
    private RedisService redisService;
    private NoteAnalyticsServiceImpl service;

    @BeforeEach
    void setUp() {
        noteMapper = mock(NoteMapper.class);
        redisService = mock(RedisService.class);
        service = new NoteAnalyticsServiceImpl(noteMapper, redisService, new ObjectMapper());
    }

    private DailyCountRow dayRow(LocalDate day, long count) {
        DailyCountRow row = new DailyCountRow();
        row.setDay(day);
        row.setDayCount(count);
        return row;
    }

    private Note note(long id, String content, int likes, int comments, int collects) {
        Note note = new Note();
        note.setNoteId(id);
        note.setAuthorId(2L);
        note.setQuestionId(9L);
        note.setContent(content);
        note.setLikeCount(likes);
        note.setCommentCount(comments);
        note.setCollectCount(collects);
        note.setAuthorName("刷题小白");
        note.setAuthorAvatarUrl("/images/a.png");
        note.setQuestionTitle("两数之和");
        note.setQuestionDifficulty(1);
        note.setCreatedAt(LocalDateTime.now());
        return note;
    }

    // ---------- 热力图 ----------

    @Test
    @DisplayName("热力图缓存未命中: 按天聚合回填位图, 统计总数/活跃天数/峰值")
    void heatmapCacheMissBuildsBitmap() {
        int year = 2026;
        LocalDate jan1 = LocalDate.of(year, 1, 1);
        when(redisService.getBytes(RedisKey.heatmap(2L, year))).thenReturn(null);
        when(noteMapper.countDailyByAuthor(eq(2L), eq(jan1), eq(LocalDate.of(year, 12, 31))))
                .thenReturn(List.of(dayRow(jan1, 2), dayRow(jan1.plusDays(3), 5)));

        HeatmapVO vo = service.getHeatmap(2L, year);

        assertEquals(year, vo.getYear());
        assertEquals(7, vo.getTotal());
        assertEquals(2, vo.getActiveDays());
        assertEquals(5, vo.getMax());
        assertEquals(365, vo.getDays().size());
        assertEquals("2026-01-01", vo.getDays().get(0).getDate());
        assertEquals(2, vo.getDays().get(0).getCount());
        assertEquals(5, vo.getDays().get(3).getCount());
        verify(redisService).setBytes(eq(RedisKey.heatmap(2L, year)), any(byte[].class),
                any(Duration.class));
    }

    @Test
    @DisplayName("热力图缓存命中: 不查库, 直接解位图(闰年 366 天)")
    void heatmapCacheHitReadsBitmap() {
        int year = 2028; // 闰年
        byte[] bitmap = new byte[366];
        bitmap[0] = 3;
        bitmap[10] = 2;
        when(redisService.getBytes(RedisKey.heatmap(2L, year))).thenReturn(bitmap);

        HeatmapVO vo = service.getHeatmap(2L, year);

        assertEquals(5, vo.getTotal());
        assertEquals(2, vo.getActiveDays());
        assertEquals(3, vo.getMax());
        assertEquals(366, vo.getDays().size());
        verify(noteMapper, never()).countDailyByAuthor(any(), any(), any());
    }

    @Test
    @DisplayName("热力图: Redis 读失败降级查库; 非法年份 40001")
    void heatmapRedisFailureFallsBack() {
        int year = 2026;
        when(redisService.getBytes(anyString())).thenThrow(new IllegalStateException("redis down"));
        when(noteMapper.countDailyByAuthor(any(), any(), any()))
                .thenReturn(List.of(dayRow(LocalDate.of(year, 5, 1), 1)));

        HeatmapVO vo = service.getHeatmap(2L, year);
        assertEquals(1, vo.getTotal());

        BusinessException e = assertThrows(BusinessException.class,
                () -> service.getHeatmap(2L, 1999));
        assertEquals(ErrorCode.BAD_PARAM, e.getErrorCode());
    }

    @Test
    @DisplayName("热力图增量: 调用位图 INCRBY; Redis 异常不抛给主流程")
    void recordHeatmapSwallowsRedisErrors() {
        LocalDate day = LocalDate.of(2026, 5, 1);
        service.recordHeatmap(2L, day, 1);
        verify(redisService).bitFieldIncrIfPresent(RedisKey.heatmap(2L, 2026), 120L * 8, 1);

        when(redisService.bitFieldIncrIfPresent(anyString(), anyLong(), anyInt()))
                .thenThrow(new IllegalStateException("redis down"));
        service.recordHeatmap(2L, day, -1); // 不抛异常
    }

    // ---------- 热度榜 ----------

    @Test
    @DisplayName("热度榜(周): 窗口聚合 → 排名/摘要/热度分, 并写缓存")
    void ranklistWeekMapsAndCaches() {
        when(redisService.get(RedisKey.noteRanklist("week", 10))).thenReturn(null);
        when(noteMapper.selectRanklistSince(any(LocalDate.class), eq(10)))
                .thenReturn(List.of(note(1L, "# 哈希表\n```java\nint x=1;\n```", 3, 2, 1),
                        note(2L, "滑动窗口", 0, 0, 0)));

        List<RankNoteVO> list = service.getRanklist("week", null);

        assertEquals(2, list.size());
        assertEquals(1, list.get(0).getRank());
        assertEquals("哈希表", list.get(0).getExcerpt());
        assertEquals(3 * 2 + 2 * 3 + 1 * 2, list.get(0).getHotScore());
        assertEquals(2, list.get(1).getRank());
        verify(redisService).set(eq(RedisKey.noteRanklist("week", 10)), anyString(),
                any(Duration.class));
    }

    @Test
    @DisplayName("热度榜(总榜): 用计数列查询 + limit 钳制到 50")
    void ranklistAllClampsLimit() {
        when(redisService.get(anyString())).thenReturn(null);
        when(noteMapper.selectRanklistAll(50)).thenReturn(List.of(note(1L, "内容", 1, 0, 0)));

        List<RankNoteVO> list = service.getRanklist("ALL", 999);

        assertEquals(1, list.size());
        verify(noteMapper).selectRanklistAll(50);
        verify(noteMapper, never()).selectRanklistSince(any(), anyInt());
    }

    @Test
    @DisplayName("热度榜: 缓存命中不查库; 非法 period → 40001")
    void ranklistCacheHitAndInvalidPeriod() {
        when(redisService.get(RedisKey.noteRanklist("month", 10)))
                .thenReturn("[{\"rank\":1,\"noteId\":5,\"hotScore\":8}]");

        List<RankNoteVO> list = service.getRanklist("month", null);
        assertEquals(5L, list.get(0).getNoteId());
        verify(noteMapper, never()).selectRanklistSince(any(), anyInt());

        BusinessException e = assertThrows(BusinessException.class,
                () -> service.getRanklist("year", 10));
        assertEquals(ErrorCode.BAD_PARAM, e.getErrorCode());
    }

    // ---------- 数据卡片 ----------

    @Test
    @DisplayName("Top3 卡片: 累计指标 + 连续提交(今天/昨天/前天)")
    void topStatsComputesStreaks() {
        NoteTotalsRow totals = new NoteTotalsRow();
        totals.setNoteCount(4);
        totals.setLikeCount(9);
        totals.setCommentCount(3);
        totals.setCollectCount(2);
        totals.setWordCount(1234);
        LocalDate today = LocalDate.now();
        when(noteMapper.selectNoteTotals(2L)).thenReturn(totals);
        when(noteMapper.selectActiveDates(2L)).thenReturn(List.of(
                today, today.minusDays(1), today.minusDays(2), today.minusDays(5)));
        when(noteMapper.countNotesSince(eq(2L), any(LocalDateTime.class))).thenReturn(2L);

        NoteStatsVO vo = service.getTopStats(2L);

        assertEquals(4, vo.getNoteCount());
        assertEquals(9, vo.getLikeCount());
        assertEquals(4, vo.getActiveDays());
        assertEquals(3, vo.getCurrentStreak());
        assertEquals(3, vo.getLongestStreak());
        assertEquals(2, vo.getMonthNoteCount());
    }

    @Test
    @DisplayName("连续提交: 断更两天当前连击归零; 空数据全零")
    void streakRules() {
        LocalDate today = LocalDate.now();
        assertEquals(0, StreakUtils.currentStreak(
                List.of(today.minusDays(2), today.minusDays(3))));
        assertEquals(1, StreakUtils.currentStreak(List.of(today.minusDays(1))));
        assertEquals(0, StreakUtils.currentStreak(List.of()));
        assertEquals(2, StreakUtils.longestStreak(
                List.of(today, today.minusDays(1), today.minusDays(4))));
        assertEquals(0, StreakUtils.longestStreak(null));
    }

    // ---------- 导出 ----------

    @Test
    @DisplayName("导出全部: 包含标题/难度/正文; 单篇仅作者可导")
    void exportNotes() {
        when(noteMapper.selectAllByAuthor(2L)).thenReturn(List.of(note(1L, "正文A", 1, 0, 0)));
        String all = new String(service.exportNotes(2L, null), java.nio.charset.StandardCharsets.UTF_8);
        assertTrue(all.contains("# notehub 笔记导出"));
        assertTrue(all.contains("## 1. 两数之和"));
        assertTrue(all.contains("- 难度: 简单"));
        assertTrue(all.contains("正文A"));

        when(noteMapper.selectById(1L)).thenReturn(note(1L, "正文A", 1, 0, 0));
        String single = new String(service.exportNotes(2L, 1L), java.nio.charset.StandardCharsets.UTF_8);
        assertTrue(single.contains("# 两数之和"));
        assertTrue(single.contains("正文A"));

        BusinessException forbidden = assertThrows(BusinessException.class,
                () -> service.exportNotes(99L, 1L));
        assertEquals(ErrorCode.FORBIDDEN, forbidden.getErrorCode());

        when(noteMapper.selectById(404L)).thenReturn(null);
        BusinessException notFound = assertThrows(BusinessException.class,
                () -> service.exportNotes(2L, 404L));
        assertEquals(ErrorCode.NOT_FOUND, notFound.getErrorCode());
    }
}
