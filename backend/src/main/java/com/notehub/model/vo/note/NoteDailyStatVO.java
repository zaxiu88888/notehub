package com.notehub.model.vo.note;

import lombok.Data;

/**
 * 笔记近 N 天互动趋势
 */
@Data
public class NoteDailyStatVO {

    private String date;

    private int likeCount;

    private int commentCount;

    private int collectCount;
}
