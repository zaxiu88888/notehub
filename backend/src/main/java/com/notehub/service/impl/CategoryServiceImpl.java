package com.notehub.service.impl;

import com.notehub.common.AfterCommit;
import com.notehub.common.BusinessException;
import com.notehub.common.ErrorCode;
import com.notehub.mapper.CategoryMapper;
import com.notehub.model.entity.Category;
import com.notehub.model.vo.category.CategoryVO;
import com.notehub.service.CategoryService;
import com.notehub.service.TwoLevelCacheService;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CategoryServiceImpl implements CategoryService {

    private final CategoryMapper categoryMapper;
    private final TwoLevelCacheService cache;

    public CategoryServiceImpl(CategoryMapper categoryMapper, TwoLevelCacheService cache) {
        this.categoryMapper = categoryMapper;
        this.cache = cache;
    }

    @Override
    public List<CategoryVO> getCategoryTree() {
        return cache.get(TwoLevelCacheService.DOMAIN_CATEGORIES, "tree", Duration.ofMinutes(5),
                new TypeReference<List<CategoryVO>>() {
                }, this::loadCategoryTree);
    }

    private List<CategoryVO> loadCategoryTree() {
        List<Category> all = categoryMapper.selectAll();
        Map<Long, CategoryVO> voMap = new HashMap<>();
        for (Category c : all) {
            CategoryVO vo = new CategoryVO();
            vo.setCategoryId(c.getCategoryId());
            vo.setName(c.getName());
            vo.setParentCategoryId(c.getParentCategoryId());
            voMap.put(c.getCategoryId(), vo);
        }
        // 两级挂树: 子挂父, 顶级保序(按 category_id)
        return all.stream()
                .map(c -> voMap.get(c.getCategoryId()))
                .filter(vo -> vo.getParentCategoryId() == null)
                .peek(top -> all.stream()
                        .filter(c -> top.getCategoryId().equals(c.getParentCategoryId()))
                        .forEach(c -> top.getChildren().add(voMap.get(c.getCategoryId()))))
                .toList();
    }

    @Override
    public CategoryVO create(String name, Long parentCategoryId) {
        validateParent(parentCategoryId);
        Category category = new Category();
        category.setName(name);
        category.setParentCategoryId(parentCategoryId);
        categoryMapper.insert(category);
        invalidateCaches();
        Category created = categoryMapper.selectById(category.getCategoryId());
        return toVO(created);
    }

    @Override
    public CategoryVO update(Long categoryId, String name, Long parentCategoryId) {
        Category existing = requireCategory(categoryId);
        validateParent(parentCategoryId);
        // 不允许把自己挂到自己/子孙下(两级结构下仅防自挂)
        if (parentCategoryId != null && parentCategoryId.equals(categoryId)) {
            throw BusinessException.of(ErrorCode.BAD_PARAM, "父分类不能是自身");
        }
        existing.setName(name != null ? name : existing.getName());
        existing.setParentCategoryId(parentCategoryId != null ? parentCategoryId : existing.getParentCategoryId());
        categoryMapper.update(existing);
        invalidateCaches();
        return toVO(categoryMapper.selectById(categoryId));
    }

    @Override
    public void delete(Long categoryId) {
        requireCategory(categoryId);
        if (categoryMapper.countChildren(categoryId) > 0) {
            throw BusinessException.of(ErrorCode.BAD_PARAM, "该分类下存在子分类, 无法删除");
        }
        if (categoryMapper.countQuestions(categoryId) > 0) {
            throw BusinessException.of(ErrorCode.BAD_PARAM, "该分类下存在题目, 无法删除");
        }
        categoryMapper.deleteById(categoryId);
        invalidateCaches();
    }

    /** 分类名会挂在题目/题库列表上, 一并失效(提交后) */
    private void invalidateCaches() {
        AfterCommit.run(() -> {
            cache.invalidate(TwoLevelCacheService.DOMAIN_CATEGORIES);
            cache.invalidate(TwoLevelCacheService.DOMAIN_QUESTIONS);
        });
    }

    private void validateParent(Long parentCategoryId) {
        if (parentCategoryId == null) {
            return; // 顶级分类
        }
        Category parent = categoryMapper.selectById(parentCategoryId);
        if (parent == null) {
            throw BusinessException.of(ErrorCode.NOT_FOUND, "父分类不存在");
        }
        if (parent.getParentCategoryId() != null) {
            throw BusinessException.of(ErrorCode.BAD_PARAM, "仅支持两级分类, 父分类必须是顶级");
        }
    }

    private Category requireCategory(Long categoryId) {
        Category category = categoryMapper.selectById(categoryId);
        if (category == null) {
            throw BusinessException.of(ErrorCode.NOT_FOUND, "分类不存在");
        }
        return category;
    }

    private CategoryVO toVO(Category category) {
        CategoryVO vo = new CategoryVO();
        vo.setCategoryId(category.getCategoryId());
        vo.setName(category.getName());
        vo.setParentCategoryId(category.getParentCategoryId());
        return vo;
    }
}
