package com.notehub.model.vo.note;

import lombok.Data;

/**
 * 个人数据卡片(Top3 主指标: 笔记数 / 收获点赞 / 连续提交天数 + 辅助指标)
 */
@Data
public class NoteStatsVO {

    private long noteCount;

    private long likeCount;

    private long commentCount;

    private long collectCount;

    /** 正文累计字符数 */
    private long wordCount;

    /** 累计有产出的天数 */
    private int activeDays;

    /** 当前连续提交天数(最近一次是今天或昨天才延续) */
    private int currentStreak;

    /** 历史最长连续提交天数 */
    private int longestStreak;

    /** 本月新增笔记数 */
    private long monthNoteCount;
}
