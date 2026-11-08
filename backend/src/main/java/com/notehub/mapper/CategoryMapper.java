package com.notehub.mapper;

import com.notehub.model.entity.Category;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CategoryMapper {

    List<Category> selectAll();

    Category selectById(Long categoryId);

    long countChildren(Long parentCategoryId);

    long countQuestions(Long categoryId);

    int insert(Category category);

    int update(Category category);

    int deleteById(Long categoryId);
}
