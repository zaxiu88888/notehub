package com.notehub.model.vo.tag;

import lombok.Data;

/**
 * 热门标签: 关联的题目/笔记数量
 */
@Data
public class TagVO {

    private Long tagId;

    private String name;

    private long questionCount;

    private long noteCount;
}
