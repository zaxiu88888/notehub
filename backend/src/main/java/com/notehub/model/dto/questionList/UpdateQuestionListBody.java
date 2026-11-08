package com.notehub.model.dto.questionList;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 更新题单(管理端), 字段均可选
 */
@Data
public class UpdateQuestionListBody {

    @Size(max = 64, message = "name 最长 64 字")
    private String name;

    @Min(value = 1, message = "type 取值 1/2")
    @Max(value = 2, message = "type 取值 1/2")
    private Integer type;

    @Size(max = 255, message = "description 最长 255 字")
    private String description;
}
