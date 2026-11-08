package com.notehub.service;

import com.notehub.model.vo.category.CategoryVO;

import java.util.List;

/**
 * 分类服务
 */
public interface CategoryService {

    /** 公开分类树(两级) */
    List<CategoryVO> getCategoryTree();

    CategoryVO create(String name, Long parentCategoryId);

    CategoryVO update(Long categoryId, String name, Long parentCategoryId);

    /** 有子分类或有题目的分类拒绝删除 */
    void delete(Long categoryId);
}
