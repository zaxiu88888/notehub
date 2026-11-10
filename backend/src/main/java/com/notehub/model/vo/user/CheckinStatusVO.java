package com.notehub.model.vo.user;

import lombok.Data;

import java.util.List;

/**
 * 每日打卡状态
 */
@Data
public class CheckinStatusVO {

    private String today;

    private boolean checkedInToday;

    private int currentStreak;

    private int longestStreak;

    private long totalDays;

    /** 近 7 天轨迹(含今天) */
    private List<Day> recent;

    @Data
    public static class Day {
        private String date;
        private boolean checked;

        public Day() {
        }

        public Day(String date, boolean checked) {
            this.date = date;
            this.checked = checked;
        }
    }
}
