package com.notehub.service;

import com.notehub.mapper.CheckinMapper;
import com.notehub.model.vo.user.CheckinStatusVO;
import com.notehub.service.impl.CheckinServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CheckinServiceImplTest {

    private CheckinMapper checkinMapper;
    private CheckinServiceImpl service;
    private final LocalDate today = LocalDate.now();

    @BeforeEach
    void setUp() {
        checkinMapper = mock(CheckinMapper.class);
        service = new CheckinServiceImpl(checkinMapper);
    }

    @Test
    @DisplayName("打卡: 幂等写入今天, 返回连续/最长/总天数")
    void checkinIsIdempotentAndReturnsStreak() {
        when(checkinMapper.insertIgnore(eq(2L), eq(today))).thenReturn(1);
        when(checkinMapper.selectDatesDesc(2L)).thenReturn(List.of(
                today, today.minusDays(1), today.minusDays(4), today.minusDays(5)));

        CheckinStatusVO vo = service.checkin(2L);

        verify(checkinMapper).insertIgnore(2L, today);
        assertTrue(vo.isCheckedInToday());
        assertEquals(2, vo.getCurrentStreak());
        assertEquals(2, vo.getLongestStreak());
        assertEquals(4, vo.getTotalDays());
        assertEquals(today.toString(), vo.getToday());
    }

    @Test
    @DisplayName("今天未打卡但昨天有: 连击仍存活, checkedInToday=false")
    void statusKeepsStreakAliveFromYesterday() {
        when(checkinMapper.selectDatesDesc(2L)).thenReturn(List.of(
                today.minusDays(1), today.minusDays(2), today.minusDays(3)));

        CheckinStatusVO vo = service.status(2L);

        assertFalse(vo.isCheckedInToday());
        assertEquals(3, vo.getCurrentStreak());
        assertEquals(3, vo.getLongestStreak());
    }

    @Test
    @DisplayName("中断超过一天: 当前连击归零, 历史最长保留")
    void brokenStreak() {
        when(checkinMapper.selectDatesDesc(2L)).thenReturn(List.of(
                today.minusDays(3), today.minusDays(4), today.minusDays(5)));

        CheckinStatusVO vo = service.status(2L);

        assertEquals(0, vo.getCurrentStreak());
        assertEquals(3, vo.getLongestStreak());
    }

    @Test
    @DisplayName("无打卡记录: 全零")
    void noCheckins() {
        when(checkinMapper.selectDatesDesc(99L)).thenReturn(List.of());
        CheckinStatusVO vo = service.status(99L);

        assertFalse(vo.isCheckedInToday());
        assertEquals(0, vo.getCurrentStreak());
        assertEquals(0, vo.getLongestStreak());
        assertEquals(0, vo.getTotalDays());
    }
}
