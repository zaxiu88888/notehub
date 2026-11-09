package com.notehub.model.dto.comment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 编辑评论(仅内容)
 */
@Data
public class CommentUpdateBody {

    @NotBlank(message = "content 不能为空")
    @Size(max = 2000, message = "content 最长 2000 字")
    private String content;
}
