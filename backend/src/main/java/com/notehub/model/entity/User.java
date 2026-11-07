package com.notehub.model.entity;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 用户实体 (表 users)
 */
@Data
public class User {

    private Long userId;

    /** 登录账号, 唯一 */
    private String account;

    /** 昵称 */
    private String username;

    /** BCrypt 散列 */
    private String password;

    /** 邮箱, 唯一 */
    private String email;

    /** 0未知 1男 2女 */
    private Integer gender;

    private LocalDate birthday;

    private String avatarUrl;

    private String school;

    private String signature;

    /** 0正常 1封禁 */
    private Integer isBanned;

    /** 0普通 1管理员 */
    private Integer isAdmin;

    private LocalDateTime lastLoginAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
