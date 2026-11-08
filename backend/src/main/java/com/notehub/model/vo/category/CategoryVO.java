package com.notehub.model.vo.category;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 分类树节点
 */
@Data
public class CategoryVO {

    private Long categoryId;

    private String name;

    private Long parentCategoryId;

    private List<CategoryVO> children = new ArrayList<>();
}
