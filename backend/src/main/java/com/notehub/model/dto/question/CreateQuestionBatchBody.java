package com.notehub.model.dto.question;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Markdown 批量导入题目
 */
@Data
public class CreateQuestionBatchBody {

    @NotNull(message = "categoryId 不能为空")
    private Long categoryId;

    @NotBlank(message = "markdown 不能为空")
    private String markdown;
}
