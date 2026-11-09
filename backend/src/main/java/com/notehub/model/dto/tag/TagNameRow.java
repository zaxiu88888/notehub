package com.notehub.model.dto.tag;

import lombok.Data;

/**
 * 批量查关联标签: owner_id 为题目/笔记 ID
 */
@Data
public class TagNameRow {

    private Long ownerId;

    private String name;
}
