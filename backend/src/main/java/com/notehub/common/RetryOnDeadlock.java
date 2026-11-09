package com.notehub.common;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记事务方法: 遇到数据库死锁/锁等待失败时自动重试
 * (点赞/取消并发在唯一索引上会短暂死锁, 重试即可成功)
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RetryOnDeadlock {

    /** 最大执行次数(含首次) */
    int maxAttempts() default 3;

    /** 退避基数(毫秒), 第 n 次重试等待 n * base */
    long backoffMillis() default 40;
}
