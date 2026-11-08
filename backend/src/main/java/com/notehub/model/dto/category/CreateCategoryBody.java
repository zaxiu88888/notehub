package com.notehub.model.dto.category;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 创建分类(管理端): parentCategoryId 为空=顶级分类
 */
@Data
public class CreateCategoryBody {

    @NotBlank(message = "name 不能为空")
    @Size(max = 32, message = "name 最长 32 字")
    private String name;

    private Long parentCategoryId;
}
