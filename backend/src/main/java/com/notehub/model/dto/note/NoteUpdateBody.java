package com.notehub.model.dto.note;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 编辑笔记(仅内容; 不支持改题目)
 */
@Data
public class NoteUpdateBody {

    @NotBlank(message = "content 不能为空")
    @Size(max = 50000, message = "content 最长 50000 字")
    private String content;
}
