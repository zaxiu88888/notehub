package com.notehub.controller;

import com.notehub.common.ApiResponse;
import com.notehub.model.dto.category.CreateCategoryBody;
import com.notehub.model.dto.category.UpdateCategoryBody;
import com.notehub.model.vo.category.CategoryVO;
import com.notehub.security.NeedAdmin;
import com.notehub.service.CategoryService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 分类 /api/categories (公开读, 管理端写)
 */
@RestController
@RequestMapping("/api")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping("/categories")
    public ApiResponse<List<CategoryVO>> getCategoryTree() {
        return ApiResponse.ok(categoryService.getCategoryTree());
    }

    @PostMapping("/admin/categories")
    @NeedAdmin
    public ApiResponse<CategoryVO> create(@Valid @RequestBody CreateCategoryBody body) {
        return ApiResponse.ok("分类已创建",
                categoryService.create(body.getName(), body.getParentCategoryId()));
    }

    @PatchMapping("/admin/categories/{categoryId}")
    @NeedAdmin
    public ApiResponse<CategoryVO> update(@PathVariable Long categoryId,
                                          @Valid @RequestBody UpdateCategoryBody body) {
        return ApiResponse.ok("分类已更新",
                categoryService.update(categoryId, body.getName(), body.getParentCategoryId()));
    }

    @DeleteMapping("/admin/categories/{categoryId}")
    @NeedAdmin
    public ApiResponse<Void> delete(@PathVariable Long categoryId) {
        categoryService.delete(categoryId);
        return ApiResponse.ok();
    }
}
