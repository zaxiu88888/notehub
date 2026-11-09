package com.notehub.model.vo.user;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 用户视图: whoami/me 返回全量(含 account/email);
 * 他人主页仅返回公开字段(不回填 account/email → 不泄露)
 */
@Data
public class UserVO {

    private Long userId;

    private String account;

    private String username;

    private String email;

    private Integer gender;

    private LocalDate birthday;

    private String avatarUrl;

    private String school;

    private String signature;

    private Integer isAdmin;

    /** 0正常 1封禁(管理端展示用) */
    private Integer isBanned;

    private LocalDateTime lastLoginAt;

    private LocalDateTime createdAt;
}
