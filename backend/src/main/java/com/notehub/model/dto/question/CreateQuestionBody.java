package com.notehub.model.dto.question;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 创建题目(管理端)
 */
@Data
public class CreateQuestionBody {

    @NotNull(message = "categoryId 不能为空")
    private Long categoryId;

    @NotBlank(message = "title 不能为空")
    @Size(max = 128, message = "title 最长 128 字")
    private String title;

    @NotNull(message = "difficulty 不能为空")
    private Integer difficulty;

    @Size(max = 128, message = "examPoint 最长 128 字")
    private String examPoint;

    private String description;
}
