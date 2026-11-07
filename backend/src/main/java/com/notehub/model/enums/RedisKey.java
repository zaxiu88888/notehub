package com.notehub.model.enums;

/**
 * Redis 键名统一管理: 冒号分层命名
 */
public final class RedisKey {

    private RedisKey() {
    }

    /** 注册验证码: email:verify_code:{email} */
    public static String verifyCode(String email) {
        return "email:verify_code:" + email;
    }

    /** 验证码发送限流: email:verify_code:limit:{email} */
    public static String verifyCodeLimit(String email) {
        return "email:verify_code:limit:" + email;
    }

    /** 邮件发送任务队列 */
    public static String emailTaskQueue() {
        return "queue:email:task";
    }
}
