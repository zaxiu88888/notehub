package com.notehub.utils;

import java.time.LocalDate;
import java.util.List;

/**
 * 连续提交计算: 入参为倒序 DISTINCT 日期
 */
public final class StreakUtils {

    private StreakUtils() {
    }

    /** 最近一次是今天/昨天才认为连击仍存活, 否则归零 */
    public static int currentStreak(List<LocalDate> datesDesc) {
        if (datesDesc == null || datesDesc.isEmpty()) {
            return 0;
        }
        LocalDate today = LocalDate.now();
        LocalDate cursor = datesDesc.get(0);
        if (cursor.isBefore(today.minusDays(1))) {
            return 0;
        }
        int streak = 1;
        for (int i = 1; i < datesDesc.size(); i++) {
            LocalDate expected = cursor.minusDays(1);
            if (datesDesc.get(i).equals(expected)) {
                streak++;
                cursor = expected;
            } else {
                break;
            }
        }
        return streak;
    }

    public static int longestStreak(List<LocalDate> datesDesc) {
        if (datesDesc == null || datesDesc.isEmpty()) {
            return 0;
        }
        int longest = 1;
        int run = 1;
        for (int i = 1; i < datesDesc.size(); i++) {
            if (datesDesc.get(i).equals(datesDesc.get(i - 1).minusDays(1))) {
                run++;
                longest = Math.max(longest, run);
            } else {
                run = 1;
            }
        }
        return longest;
    }
}
