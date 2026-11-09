package com.notehub.model.dto.note;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

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

    /** 不传 = 继承题目标签; 传空列表 = 不使用标签 */
    @Size(max = 5, message = "最多 5 个标签")
    private List<@Size(max = 20, message = "标签最长 20 个字符") String> tags;
}
