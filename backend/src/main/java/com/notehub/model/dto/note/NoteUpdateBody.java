package com.notehub.model.dto.note;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 编辑笔记(仅内容/标签; 不支持改题目)
 */
@Data
public class NoteUpdateBody {

    @NotBlank(message = "content 不能为空")
    @Size(max = 50000, message = "content 最长 50000 字")
    private String content;

    /** null = 不改标签(兼容旧客户端); 空列表 = 清空 */
    @Size(max = 5, message = "最多 5 个标签")
    private List<@Size(max = 20, message = "标签最长 20 个字符") String> tags;
}
