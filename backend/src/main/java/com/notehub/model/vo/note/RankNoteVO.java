package com.notehub.model.vo.note;

import lombok.Data;

/**
 * 笔记热度榜条目: 榜单统计窗口内的互动数 + 热度分
 */
@Data
public class RankNoteVO {

    private int rank;

    private Long noteId;

    private Long questionId;

    private String questionTitle;

    private Integer difficulty;

    private NoteVO.AuthorVO author;

    /** Markdown 去标记后的摘要 */
    private String excerpt;

    /** 以下计数均为统计窗口内(period=all 时为累计) */
    private int likeCount;

    private int commentCount;

    private int collectCount;

    /** 热度分 = 赞*2 + 评论*3 + 收藏*2 */
    private long hotScore;
}
