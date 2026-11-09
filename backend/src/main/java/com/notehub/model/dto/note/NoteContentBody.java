package com.notehub.model.dto.note;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 发布/编辑笔记
 */
@Data
public class NoteContentBody {

    @NotNull(message = "questionId 不能为空")
    private Long questionId;

    @NotBlank(message = "content 不能为空")
    @Size(max = 50000, message = "content 最长 50000 字")
    private String content;
}
