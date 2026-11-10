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

    /** 刷题热力图位图: heatmap:{userId}:{year} (每天 8 bit 计数) */
    public static String heatmap(Long userId, int year) {
        return "heatmap:" + userId + ":" + year;
    }

    /** 笔记热度榜缓存: rank:notes:{period}:{limit} */
    public static String noteRanklist(String period, int limit) {
        return "rank:notes:" + period + ":" + limit;
    }

    /** AI 总结缓存: ai:summary:{noteId} */
    public static String aiSummary(Long noteId) {
        return "ai:summary:" + noteId;
    }
}
