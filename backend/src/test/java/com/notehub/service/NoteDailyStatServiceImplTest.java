package com.notehub.service;

import com.notehub.common.BusinessException;
import com.notehub.common.ErrorCode;
import com.notehub.mapper.NoteDailyStatMapper;
import com.notehub.mapper.NoteMapper;
import com.notehub.model.entity.Note;
import com.notehub.model.entity.NoteDailyStat;
import com.notehub.model.vo.note.NoteDailyStatVO;
import com.notehub.service.impl.NoteDailyStatServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NoteDailyStatServiceImplTest {

    private NoteDailyStatMapper statMapper;
    private NoteMapper noteMapper;
    private NoteDailyStatServiceImpl service;

    @BeforeEach
    void setUp() {
        statMapper = mock(NoteDailyStatMapper.class);
        noteMapper = mock(NoteMapper.class);
        service = new NoteDailyStatServiceImpl(statMapper, noteMapper);
    }

    @Test
    @DisplayName("记录互动: delta=0 不落库; 非 0 按今天 upsert")
    void recordSkipsZeroDelta() {
        service.recordLike(1L, 0);
        verify(statMapper, never()).adjustLike(any(), any(), eq(0));

        service.recordLike(1L, 1);
        service.recordComment(1L, -2);
        service.recordCollect(1L, 1);
        verify(statMapper).adjustLike(1L, LocalDate.now(), 1);
        verify(statMapper).adjustComment(1L, LocalDate.now(), -2);
        verify(statMapper).adjustCollect(1L, LocalDate.now(), 1);
    }

    @Test
    @DisplayName("趋势: days 钳制到 30, 返回逐日数据")
    void recentClampsDaysAndMaps() {
        when(noteMapper.selectById(10L)).thenReturn(new Note());
        NoteDailyStat stat = new NoteDailyStat();
        stat.setNoteId(10L);
        stat.setStatDate(LocalDate.now());
        stat.setLikeCount(2);
        stat.setCommentCount(3);
        stat.setCollectCount(1);
        when(statMapper.selectSince(eq(10L), any(LocalDate.class))).thenReturn(List.of(stat));

        List<NoteDailyStatVO> list = service.recent(10L, 999);

        assertEquals(1, list.size());
        assertEquals(LocalDate.now().toString(), list.get(0).getDate());
        assertEquals(2, list.get(0).getLikeCount());
        assertEquals(3, list.get(0).getCommentCount());
        assertEquals(1, list.get(0).getCollectCount());
        verify(statMapper).selectSince(10L, LocalDate.now().minusDays(29));
    }

    @Test
    @DisplayName("笔记不存在 → 40401")
    void recentNotFound() {
        when(noteMapper.selectById(404L)).thenReturn(null);

        BusinessException e = assertThrows(BusinessException.class,
                () -> service.recent(404L, 7));
        assertEquals(ErrorCode.NOT_FOUND, e.getErrorCode());
    }
}
