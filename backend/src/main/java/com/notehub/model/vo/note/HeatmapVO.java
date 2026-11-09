package com.notehub.model.vo.note;

import lombok.Data;

import java.util.List;

/**
 * 刷题热力图(GitHub 风格): 一年 365/366 天, 每天笔记产出数
 */
@Data
public class HeatmapVO {

    private int year;

    /** 全年笔记总数 */
    private long total;

    /** 有产出的天数 */
    private int activeDays;

    /** 单日最大产出 */
    private int max;

    /** 全年逐日数据(含 0, 前端直接铺格子) */
    private List<Day> days;

    @Data
    public static class Day {
        private String date;
        private int count;

        public Day() {
        }

        public Day(String date, int count) {
            this.date = date;
            this.count = count;
        }
    }
}
