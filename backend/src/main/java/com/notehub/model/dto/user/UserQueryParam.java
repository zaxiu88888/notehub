package com.notehub.model.dto.user;

import lombok.Data;

/**
 * 管理端用户列表查询参数
 */
@Data
public class UserQueryParam {

    private String account;

    /** 0正常 1封禁; null=全部 */
    private Integer banned;

    private int page = 1;

    private int pageSize = 10;
}
