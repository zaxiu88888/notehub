package com.notehub.model.vo.comment;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 评论视图: 顶层评论 + 楼中楼回复树
 */
@Data
public class CommentVO {

    private Long commentId;

    private Long noteId;

    private Long parentId;

    private Long replyToId;

    private String content;

    private Integer likeCount;

    private Integer replyCount;

    private Integer isDeleted;

    private LocalDateTime createdAt;

    private AuthorVO author;

    /** 楼中楼: 回复目标用户名(仅回复有) */
    private String replyToUserName;

    private boolean liked;

    private List<CommentVO> replies = new ArrayList<>();

    @Data
    public static class AuthorVO {
        private Long userId;
        private String username;
        private String avatarUrl;
    }
}
