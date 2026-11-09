package com.notehub.model.vo.note;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 笔记视图: 聚合作者摘要+题目摘要+我的互动状态
 */
@Data
public class NoteVO {

    private Long noteId;

    private Long questionId;

    private String content;

    private Integer likeCount;

    private Integer commentCount;

    private Integer collectCount;

    private LocalDateTime createdAt;

    private AuthorVO author;

    private QuestionVO question;

    private MyActionsVO myActions;

    @Data
    public static class AuthorVO {
        private Long userId;
        private String username;
        private String avatarUrl;
    }

    @Data
    public static class QuestionVO {
        private Long questionId;
        private String title;
        private Integer difficulty;
    }

    /** 未登录恒 false */
    @Data
    public static class MyActionsVO {
        private boolean liked;
        private boolean collected;
    }
}
