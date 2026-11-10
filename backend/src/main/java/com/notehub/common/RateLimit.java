package com.notehub.common;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * I5 接口限流: 固定窗口计数(Redis INCR + PEXPIRE)
 * 维度 = 登录用户 ID / 未登录客户端 IP; 超过限制抛 42901
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {

    /** 同一标识在窗口内允许的次数 */
    int limit();

    /** 窗口秒数, 默认 60 */
    int windowSeconds() default 60;

    /** 限流作用域(默认用方法签名); 同类接口建议显式指定统一 key */
    String key() default "";
}
