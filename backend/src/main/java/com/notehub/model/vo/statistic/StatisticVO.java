package com.notehub.model.vo.statistic;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * 管理看板: 今日实时指标 + 历史趋势
 */
@Data
public class StatisticVO {

    private Today today;

    /** 最近 N 天快照(按日期升序, 便于前端画折线) */
    private List<Point> trend;

    @Data
    public static class Today {
        private long loginCount;
        private long registerCount;
        private long totalRegisterCount;
        private long noteCount;
        private long submitNoteCount;
        private long totalNoteCount;
    }

    @Data
    public static class Point {
        private LocalDate date;
        private Integer loginCount;
        private Integer registerCount;
        private Integer noteCount;
    }
}
