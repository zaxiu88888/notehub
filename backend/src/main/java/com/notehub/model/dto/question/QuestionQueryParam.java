package com.notehub.model.dto.question;

import lombok.Data;

/**
 * 题目列表查询参数
 */
@Data
public class QuestionQueryParam {

    private Long categoryId;

    /** 1简单 2中等 3困难 */
    private Integer difficulty;

    /** 标签名(精确匹配, 大小写不敏感) */
    private String tag;

    private int page = 1;

    private int pageSize = 10;
}
