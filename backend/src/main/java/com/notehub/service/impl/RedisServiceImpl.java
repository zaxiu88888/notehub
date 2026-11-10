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
import java.util.UUID;

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

    /**
     * 滑动窗口限流(原子):
     * 1) 删除 score <= now-window 的成员; 2) 数量 < limit 才写入并放行
     * Redis TIME 取服务端毫秒, 避免客户端时钟偏差
     */
    private static final DefaultRedisScript<Long> SLIDING_WINDOW_SCRIPT = new DefaultRedisScript<>(
            "local t = redis.call('TIME') " +
                    "local now = tonumber(t[1]) * 1000 + math.floor(tonumber(t[2]) / 1000) " +
                    "local window = tonumber(ARGV[1]) " +
                    "local limit = tonumber(ARGV[2]) " +
                    "redis.call('ZREMRANGEBYSCORE', KEYS[1], 0, now - window) " +
                    "if redis.call('ZCARD', KEYS[1]) < limit then " +
                    "  redis.call('ZADD', KEYS[1], now, t[1] .. ':' .. t[2] .. ':' .. ARGV[3]) " +
                    "  redis.call('PEXPIRE', KEYS[1], window) " +
                    "  return 1 " +
                    "end " +
                    "return 0",
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
    public long increment(String key) {
        Long value = stringRedisTemplate.opsForValue().increment(key);
        return value == null ? 0L : value;
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

    @Override
    public boolean slidingWindowAcquire(String key, int limit, Duration window) {
        Long result = stringRedisTemplate.execute(SLIDING_WINDOW_SCRIPT, List.of(key),
                String.valueOf(window.toMillis()), String.valueOf(limit),
                UUID.randomUUID().toString());
        return result != null && result == 1L;
    }

    private byte[] rawKey(String key) {
        return key.getBytes(StandardCharsets.UTF_8);
    }
}
