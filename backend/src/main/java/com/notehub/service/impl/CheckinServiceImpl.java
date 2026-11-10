package com.notehub.service.impl;

import com.notehub.mapper.CheckinMapper;
import com.notehub.model.vo.user.CheckinStatusVO;
import com.notehub.service.CheckinService;
import com.notehub.utils.StreakUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class CheckinServiceImpl implements CheckinService {

    private final CheckinMapper checkinMapper;

    public CheckinServiceImpl(CheckinMapper checkinMapper) {
        this.checkinMapper = checkinMapper;
    }

    @Override
    public CheckinStatusVO checkin(Long userId) {
        checkinMapper.insertIgnore(userId, LocalDate.now());
        return status(userId);
    }

    @Override
    public CheckinStatusVO status(Long userId) {
        List<LocalDate> dates = checkinMapper.selectDatesDesc(userId);
        LocalDate today = LocalDate.now();

        CheckinStatusVO vo = new CheckinStatusVO();
        vo.setToday(today.toString());
        vo.setCheckedInToday(!dates.isEmpty() && today.equals(dates.get(0)));
        vo.setCurrentStreak(StreakUtils.currentStreak(dates));
        vo.setLongestStreak(StreakUtils.longestStreak(dates));
        vo.setTotalDays(dates.size());

        Set<LocalDate> checked = new HashSet<>(dates);
        List<CheckinStatusVO.Day> recent = new ArrayList<>(7);
        for (int i = 6; i >= 0; i--) {
            LocalDate day = today.minusDays(i);
            recent.add(new CheckinStatusVO.Day(day.toString(), checked.contains(day)));
        }
        vo.setRecent(recent);
        return vo;
    }
}
