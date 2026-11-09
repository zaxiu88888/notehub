package com.notehub.model.dto.collection;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 创建收藏夹
 */
@Data
public class CreateCollectionBody {

    @NotBlank(message = "name 不能为空")
    @Size(max = 32, message = "name 最长 32 字")
    private String name;

    @Size(max = 128, message = "description 最长 128 字")
    private String description;
}
