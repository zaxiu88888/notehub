package com.notehub.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.notehub.service.RedisService;
import com.notehub.service.TwoLevelCacheService;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

@Log4j2
@Service
public class TwoLevelCacheServiceImpl implements TwoLevelCacheService {

    private static final Duration LOCAL_TTL = Duration.ofSeconds(30);
    private static final long LOCAL_MAX_SIZE = 1000;
    /** 跨实例共享版本号: cache:version:{domain} */
    private static final String VERSION_KEY_PREFIX = "cache:version:";

    private final Cache<String, Object> local = Caffeine.newBuilder()
            .maximumSize(LOCAL_MAX_SIZE)
            .expireAfterWrite(LOCAL_TTL)
            .build();
    /** Redis 不可用时的本机兜底版本 */
    private final ConcurrentHashMap<String, AtomicLong> localVersions = new ConcurrentHashMap<>();
    private final RedisService redisService;
    private final ObjectMapper objectMapper;

    public TwoLevelCacheServiceImpl(RedisService redisService, ObjectMapper objectMapper) {
        this.redisService = redisService;
        this.objectMapper = objectMapper;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(String domain, String suffix, Duration ttl,
                     TypeReference<T> type, Supplier<T> loader) {
        String key = key(domain, suffix);
        // Caffeine.get(key, loader): 同一 key 并发未命中只执行一次回源(防击穿)
        Object value = local.get(key, cacheKey -> {
            try {
                String raw = redisService.get(cacheKey);
                if (raw != null) {
                    return objectMapper.readValue(raw, type);
                }
            } catch (Exception e) {
                log.warn("[缓存] L2 读取失败, 回源 DB: domain={}, err={}", domain, e.getMessage());
            }
            T loaded = loader.get();
            if (loaded != null) {
                try {
                    redisService.set(cacheKey, objectMapper.writeValueAsString(loaded), ttl);
                } catch (Exception e) {
                    log.warn("[缓存] L2 写入失败: domain={}, err={}", domain, e.getMessage());
                }
            }
            return loaded;
        });
        return (T) value;
    }

    @Override
    public void invalidate(String domain) {
        try {
            redisService.increment(VERSION_KEY_PREFIX + domain);
        } catch (Exception e) {
            // Redis 故障时至少保证本机失效, 其他节点等 Redis 恢复后按新版本回源
            localVersions.computeIfAbsent(domain, key -> new AtomicLong()).incrementAndGet();
            log.warn("[缓存] 推进 Redis 版本失败(本机兜底): domain={}, err={}", domain, e.getMessage());
        }
        // 本地缓存规模小(<=1000), 直接全清比逐 key 匹配更简单可靠
        local.invalidateAll();
        log.debug("[缓存] 失效: domain={}", domain);
    }

    /**
     * 版本号存 Redis, 多实例共享: 任一节点失效后, 其他节点下次请求
     * 会算出新 key 而绕过旧 L1(旧条目 30s 后自然淘汰)。
     * Redis 不可用时退回本机版本, 保证单机行为不变。
     */
    private long version(String domain) {
        try {
            String raw = redisService.get(VERSION_KEY_PREFIX + domain);
            if (raw != null) {
                return Long.parseLong(raw);
            }
        } catch (Exception e) {
            log.warn("[缓存] 读取版本失败(用本机版本): domain={}, err={}", domain, e.getMessage());
        }
        return localVersions.computeIfAbsent(domain, key -> new AtomicLong()).get();
    }

    private String key(String domain, String suffix) {
        return "cache:" + domain + ":v" + version(domain) + ":" + suffix;
    }
}
