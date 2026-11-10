package com.notehub.model.entity;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 笔记每日互动统计 (表 note_daily_stats, 主键 note_id + stat_date 去重)
 */
@Data
public class NoteDailyStat {

    private Long noteId;

    private LocalDate statDate;

    private Integer likeCount;

    private Integer commentCount;

    private Integer collectCount;

    private LocalDateTime updatedAt;
}
