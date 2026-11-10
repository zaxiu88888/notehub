package com.notehub.model.vo.user;

import lombok.Data;

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
}
