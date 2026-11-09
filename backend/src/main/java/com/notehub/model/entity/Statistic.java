package com.notehub.model.entity;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 每日统计快照 (表 statistics, uk: date 幂等)
 */
@Data
public class Statistic {

    private Long id;

    /** 当日登录次数(以 last_login_at 计) */
    private Integer loginCount;

    /** 当日注册数 */
    private Integer registerCount;

    /** 累计注册数 */
    private Integer totalRegisterCount;

    /** 当日新增笔记数 */
    private Integer noteCount;

    /** 当日提交笔记人数(去重) */
    private Integer submitNoteCount;

    /** 累计笔记数 */
    private Integer totalNoteCount;

    private LocalDate date;

    private LocalDateTime createdAt;
}
