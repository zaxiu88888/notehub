package com.notehub.service;

import java.time.Duration;

/**
 * Redis 薄封装: 只暴露本业务需要的原语, 便于单测 mock
 */
public interface RedisService {

    String get(String key);

    void set(String key, String value, Duration ttl);

    /** 不存在才写入(限流用); 返回 false 表示已存在 */
    boolean setIfAbsent(String key, String value, Duration ttl);

    void delete(String key);

    /** 队列左进右出 */
    void leftPush(String key, String value);

    String rightPop(String key);
}
