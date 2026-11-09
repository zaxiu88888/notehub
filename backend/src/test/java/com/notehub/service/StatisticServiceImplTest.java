package com.notehub.service;

import com.notehub.mapper.NoteMapper;
import com.notehub.mapper.StatisticMapper;
import com.notehub.mapper.UserMapper;
import com.notehub.model.entity.Statistic;
import com.notehub.model.vo.statistic.StatisticVO;
import com.notehub.service.impl.StatisticServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StatisticServiceImplTest {

    private UserMapper userMapper;
    private NoteMapper noteMapper;
    private StatisticMapper statisticMapper;
    private StatisticServiceImpl service;

    @BeforeEach
    void setUp() {
        userMapper = mock(UserMapper.class);
        noteMapper = mock(NoteMapper.class);
        statisticMapper = mock(StatisticMapper.class);
        service = new StatisticServiceImpl(userMapper, noteMapper, statisticMapper);
    }

    @Test
    @DisplayName("看板: 今日6指标实时聚合 + 趋势按日期升序(倒序反转)")
    void dashboardAggregatesTodayAndReversesTrend() {
        when(userMapper.countTodayLogin()).thenReturn(3L);
        when(userMapper.countTodayRegister()).thenReturn(2L);
        when(userMapper.countTotal()).thenReturn(10L);
        when(noteMapper.countTodayNotes()).thenReturn(5L);
        when(noteMapper.countTodayAuthors()).thenReturn(4L);
        when(noteMapper.countTotalNotes()).thenReturn(20L);

        Statistic d1 = statistic(LocalDate.of(2026, 11, 7), 1, 1, 2);
        Statistic d2 = statistic(LocalDate.of(2026, 11, 8), 2, 2, 3);
        when(statisticMapper.listRecent(30)).thenReturn(List.of(d2, d1)); // DB 倒序

        StatisticVO vo = service.getDashboard(30);

        assertEquals(3L, vo.getToday().getLoginCount());
        assertEquals(20L, vo.getToday().getTotalNoteCount());
        assertEquals(2, vo.getTrend().size());
        assertEquals(LocalDate.of(2026, 11, 7), vo.getTrend().get(0).getDate()); // 升序
        assertEquals(3, vo.getTrend().get(1).getNoteCount());
    }

    @Test
    @DisplayName("看板: 趋势天数收敛到 1~90")
    void dashboardClampsDays() {
        when(statisticMapper.listRecent(90)).thenReturn(List.of());
        service.getDashboard(999);
        verify(statisticMapper).listRecent(90);

        when(statisticMapper.listRecent(1)).thenReturn(List.of());
        service.getDashboard(0);
        verify(statisticMapper).listRecent(1);
    }

    @Test
    @DisplayName("快照: 6 项指标 + 今日日期, 走幂等 upsert")
    void snapshotTodayUpserts() {
        when(userMapper.countTodayLogin()).thenReturn(3L);
        when(userMapper.countTodayRegister()).thenReturn(2L);
        when(userMapper.countTotal()).thenReturn(10L);
        when(noteMapper.countTodayNotes()).thenReturn(5L);
        when(noteMapper.countTodayAuthors()).thenReturn(4L);
        when(noteMapper.countTotalNotes()).thenReturn(20L);

        Statistic snapshot = service.snapshotToday();

        ArgumentCaptor<Statistic> captor = ArgumentCaptor.forClass(Statistic.class);
        verify(statisticMapper).upsert(captor.capture());
        Statistic saved = captor.getValue();
        assertEquals(LocalDate.now(), saved.getDate());
        assertEquals(3, saved.getLoginCount());
        assertEquals(4, saved.getSubmitNoteCount());
        assertEquals(20, saved.getTotalNoteCount());
        assertEquals(LocalDate.now(), snapshot.getDate());
    }

    private Statistic statistic(LocalDate date, int login, int register, int note) {
        Statistic s = new Statistic();
        s.setDate(date);
        s.setLoginCount(login);
        s.setRegisterCount(register);
        s.setNoteCount(note);
        return s;
    }
}
