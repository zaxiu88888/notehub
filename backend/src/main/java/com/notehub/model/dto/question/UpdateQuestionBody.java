package com.notehub.model.dto.question;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 更新题目(管理端), 字段均可选
 */
@Data
public class UpdateQuestionBody {

    private Long categoryId;

    @Size(max = 128, message = "title 最长 128 字")
    private String title;

    @Min(value = 1, message = "difficulty 取值 1/2/3")
    @Max(value = 3, message = "difficulty 取值 1/2/3")
    private Integer difficulty;

    @Size(max = 128, message = "examPoint 最长 128 字")
    private String examPoint;

    private String description;
}
