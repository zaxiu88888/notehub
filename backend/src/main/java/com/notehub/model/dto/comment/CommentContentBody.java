package com.notehub.model.dto.comment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 发布/编辑评论
 */
@Data
public class CommentContentBody {

    @NotNull(message = "noteId 不能为空")
    private Long noteId;

    /** 回复目标评论(楼中楼); 顶层评论为 null */
    private Long parentId;

    @NotBlank(message = "content 不能为空")
    @Size(max = 2000, message = "content 最长 2000 字")
    private String content;
}
