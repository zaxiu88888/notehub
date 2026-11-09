package com.notehub.model.dto.message;

import lombok.Data;

/**
 * 消息列表查询参数
 */
@Data
public class MessageQueryParams {

    /** 接收者(service 层回填, 不接受前端传入) */
    private Long receiverId;

    /** 1点赞 2评论 3系统; null=全部 */
    private Integer type;

    private int page = 1;

    private int pageSize = 10;
}
