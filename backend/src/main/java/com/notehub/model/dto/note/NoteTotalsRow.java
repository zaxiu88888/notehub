package com.notehub.model.dto.note;

import lombok.Data;

/**
 * 个人笔记累计指标(一条聚合 SQL)
 */
@Data
public class NoteTotalsRow {

    private long noteCount;

    private long likeCount;

    private long commentCount;

    private long collectCount;

    private long wordCount;
}
