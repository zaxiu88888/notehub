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

    // ---- I2 热力图位图(二进制读写, 走 RedisConnection 绕开 JSON 序列化) ----

    byte[] getBytes(String key);

    void setBytes(String key, byte[] value, Duration ttl);

    /**
     * 位图计数原子自增: 仅当 key 已存在时执行, 不存在返回 false(等下次读时从库回填)
     * 溢出饱和(SAT), 不会因取消笔记而回绕成 255
     */
    boolean bitFieldIncrIfPresent(String key, long bitOffset, long delta);
}
