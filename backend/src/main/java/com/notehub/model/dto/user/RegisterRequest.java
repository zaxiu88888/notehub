package com.notehub.model.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 注册请求: 邮箱验证码 + 账号密码
 */
@Data
public class RegisterRequest {

    @NotBlank(message = "email 不能为空")
    @Email(message = "email 格式不正确")
    private String email;

    @NotBlank(message = "code 不能为空")
    @Pattern(regexp = "\\d{6}", message = "code 必须为 6 位数字")
    private String code;

    @NotBlank(message = "account 不能为空")
    @Pattern(regexp = "[A-Za-z0-9_]{4,20}", message = "account 须为 4-20 位字母/数字/下划线")
    private String account;

    @NotBlank(message = "username 不能为空")
    @Size(min = 1, max = 30, message = "username 长度须为 1-30")
    private String username;

    @NotBlank(message = "password 不能为空")
    @Size(min = 6, max = 64, message = "password 长度须为 6-64")
    private String password;
}
