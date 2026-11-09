package com.notehub.service.impl;

import com.notehub.service.RedisService;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

@Service
public class RedisServiceImpl implements RedisService {

    /**
     * 位图计数自增: key 不存在直接返回 0(不无中生有, 避免生成半截位图);
     * OVERFLOW SAT 保证取消笔记减到 0 后不会回绕成 255
     */
    private static final DefaultRedisScript<Long> BITFIELD_INCR_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('EXISTS', KEYS[1]) == 0 then return 0 end " +
                    "redis.call('BITFIELD', KEYS[1], 'OVERFLOW', 'SAT', 'INCRBY', 'u8', ARGV[1], ARGV[2]) " +
                    "return 1",
            Long.class);

    private final RedisTemplate<String, Object> redisTemplate;
    private final StringRedisTemplate stringRedisTemplate;

    public RedisServiceImpl(RedisTemplate<String, Object> redisTemplate,
                            StringRedisTemplate stringRedisTemplate) {
        this.redisTemplate = redisTemplate;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public String get(String key) {
        Object value = redisTemplate.opsForValue().get(key);
        return value == null ? null : String.valueOf(value);
    }

    @Override
    public void set(String key, String value, Duration ttl) {
        redisTemplate.opsForValue().set(key, value, ttl);
    }

    @Override
    public boolean setIfAbsent(String key, String value, Duration ttl) {
        Boolean ok = redisTemplate.opsForValue().setIfAbsent(key, value, ttl);
        return Boolean.TRUE.equals(ok);
    }

    @Override
    public void delete(String key) {
        redisTemplate.delete(key);
    }

    @Override
    public void leftPush(String key, String value) {
        redisTemplate.opsForList().leftPush(key, value);
    }

    @Override
    public String rightPop(String key) {
        Object value = redisTemplate.opsForList().rightPop(key);
        return value == null ? null : String.valueOf(value);
    }

    @Override
    public byte[] getBytes(String key) {
        // 走原始连接, 避免 GenericJackson2Json 序列化破坏二进制位图
        return redisTemplate.execute((RedisCallback<byte[]>) connection ->
                connection.stringCommands().get(rawKey(key)));
    }

    @Override
    public void setBytes(String key, byte[] value, Duration ttl) {
        redisTemplate.execute((RedisCallback<Void>) connection -> {
            connection.stringCommands().setEx(rawKey(key), ttl.getSeconds(), value);
            return null;
        });
    }

    @Override
    public boolean bitFieldIncrIfPresent(String key, long offset, long delta) {
        Long result = stringRedisTemplate.execute(BITFIELD_INCR_SCRIPT, List.of(key),
                String.valueOf(offset), String.valueOf(delta));
        return result != null && result == 1L;
    }

    private byte[] rawKey(String key) {
        return key.getBytes(StandardCharsets.UTF_8);
    }
}
