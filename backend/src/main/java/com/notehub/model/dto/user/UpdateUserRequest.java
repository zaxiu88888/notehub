package com.notehub.model.dto.user;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

/**
 * 修改个人资料: account/email/password 不可通过此接口修改
 */
@Data
public class UpdateUserRequest {

    @Size(min = 1, max = 30, message = "username 长度须为 1-30")
    private String username;

    @Min(value = 0, message = "gender 取值 0/1/2")
    @Max(value = 2, message = "gender 取值 0/1/2")
    private Integer gender;

    private LocalDate birthday;

    @Size(max = 64, message = "school 最长 64 字")
    private String school;

    @Size(max = 128, message = "signature 最长 128 字")
    private String signature;
}
