package com.notehub.model.dto.note;

import lombok.Data;

import java.time.LocalDate;

/**
 * 按天聚合的笔记数(热力图回填数据源)
 */
@Data
public class DailyCountRow {

    private LocalDate day;

    private long dayCount;
}
