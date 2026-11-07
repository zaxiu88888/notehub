package com.notehub.model.vo.user;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 注册/登录成功: 立即下发 token
 */
@Data
@AllArgsConstructor
public class LoginUserVO {

    private Long userId;

    private String token;
}
