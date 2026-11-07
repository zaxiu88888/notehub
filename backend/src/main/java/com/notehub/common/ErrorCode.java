package com.notehub.common;

import lombok.Getter;

/**
 * 全局错误码表 (docs/technical-design.md §7)
 */
@Getter
public enum ErrorCode {

    SUCCESS(200, "success"),

    /** 参数 */
    BAD_PARAM(40001, "参数校验失败"),

    /** 认证与权限 */
    UNAUTHORIZED(40101, "用户未登录"),
    ADMIN_REQUIRED(40102, "需要管理员权限"),
    FORBIDDEN(40301, "无权操作该资源"),

    /** 资源 */
    NOT_FOUND(40401, "资源不存在"),

    /** 冲突 */
    DUPLICATED(40901, "数据已存在"),

    /** 验证码/邮件 */
    CODE_INVALID(41001, "验证码错误或已过期"),
    CODE_TOO_FREQUENT(41002, "发送验证码过于频繁"),

    /** 限流 */
    TOO_MANY_REQUESTS(42901, "请求过于频繁"),

    /** 系统 */
    INTERNAL_ERROR(50000, "服务器内部错误");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
