package com.notehub.model.dto.category;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 更新分类(管理端), 字段均可选
 */
@Data
public class UpdateCategoryBody {

    @Size(max = 32, message = "name 最长 32 字")
    private String name;

    private Long parentCategoryId;
}
