package com.notehub.service.impl;

import com.notehub.mapper.NoteMapper;
import com.notehub.mapper.StatisticMapper;
import com.notehub.mapper.UserMapper;
import com.notehub.model.entity.Statistic;
import com.notehub.model.vo.statistic.StatisticVO;
import com.notehub.service.StatisticService;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Log4j2
@Service
public class StatisticServiceImpl implements StatisticService {

    private final UserMapper userMapper;
    private final NoteMapper noteMapper;
    private final StatisticMapper statisticMapper;

    public StatisticServiceImpl(UserMapper userMapper, NoteMapper noteMapper,
                                StatisticMapper statisticMapper) {
        this.userMapper = userMapper;
        this.noteMapper = noteMapper;
        this.statisticMapper = statisticMapper;
    }

    @Override
    public StatisticVO getDashboard(int trendDays) {
        int days = Math.min(Math.max(trendDays, 1), 90);

        StatisticVO.Today today = new StatisticVO.Today();
        today.setLoginCount(userMapper.countTodayLogin());
        today.setRegisterCount(userMapper.countTodayRegister());
        today.setTotalRegisterCount(userMapper.countTotal());
        today.setNoteCount(noteMapper.countTodayNotes());
        today.setSubmitNoteCount(noteMapper.countTodayAuthors());
        today.setTotalNoteCount(noteMapper.countTotalNotes());

        // 快照表按日期倒序取, 反转成升序给前端画图
        List<Statistic> recent = new ArrayList<>(statisticMapper.listRecent(days));
        Collections.reverse(recent);
        List<StatisticVO.Point> trend = recent.stream().map(s -> {
            StatisticVO.Point p = new StatisticVO.Point();
            p.setDate(s.getDate());
            p.setLoginCount(s.getLoginCount());
            p.setRegisterCount(s.getRegisterCount());
            p.setNoteCount(s.getNoteCount());
            return p;
        }).toList();

        StatisticVO vo = new StatisticVO();
        vo.setToday(today);
        vo.setTrend(trend);
        return vo;
    }

    @Override
    public Statistic snapshotToday() {
        Statistic statistic = new Statistic();
        statistic.setLoginCount((int) userMapper.countTodayLogin());
        statistic.setRegisterCount((int) userMapper.countTodayRegister());
        statistic.setTotalRegisterCount((int) userMapper.countTotal());
        statistic.setNoteCount((int) noteMapper.countTodayNotes());
        statistic.setSubmitNoteCount((int) noteMapper.countTodayAuthors());
        statistic.setTotalNoteCount((int) noteMapper.countTotalNotes());
        statistic.setDate(LocalDate.now());

        statisticMapper.upsert(statistic);
        log.info("[统计] 当日快照已落库: date={}, login={}, register={}, note={}",
                statistic.getDate(), statistic.getLoginCount(),
                statistic.getRegisterCount(), statistic.getNoteCount());
        return statistic;
    }
}
