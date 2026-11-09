package com.notehub.model.dto.note;

import lombok.Data;

/**
 * 笔记列表查询参数
 */
@Data
public class NoteQueryParams {

    private Long questionId;

    private Long authorId;

    /** latest=最新(默认) hot=热度(点赞+评论+收藏加权) */
    private String sort = "latest";

    private int page = 1;

    private int pageSize = 10;
}
