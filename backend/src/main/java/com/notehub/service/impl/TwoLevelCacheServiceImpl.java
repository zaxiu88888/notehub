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

    private final Cache<String, Object> local = Caffeine.newBuilder()
            .maximumSize(LOCAL_MAX_SIZE)
            .expireAfterWrite(LOCAL_TTL)
            .build();
    private final ConcurrentHashMap<String, AtomicLong> versions = new ConcurrentHashMap<>();
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
        versions.computeIfAbsent(domain, key -> new AtomicLong()).incrementAndGet();
        // 本地缓存规模小(<=1000), 直接全清比逐 key 匹配更简单可靠
        local.invalidateAll();
        log.debug("[缓存] 失效: domain={}", domain);
    }

    private String key(String domain, String suffix) {
        long version = versions.computeIfAbsent(domain, key -> new AtomicLong()).get();
        return "cache:" + domain + ":v" + version + ":" + suffix;
    }
}
