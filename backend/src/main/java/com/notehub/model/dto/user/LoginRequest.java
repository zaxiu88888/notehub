package com.notehub.model.dto.user;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 登录请求: 账号 + 密码
 */
@Data
public class LoginRequest {

    @NotBlank(message = "account 不能为空")
    private String account;

    @NotBlank(message = "password 不能为空")
    private String password;
}
