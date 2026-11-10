package com.notehub.service;

import com.fasterxml.jackson.core.type.TypeReference;

import java.time.Duration;
import java.util.function.Supplier;

/**
 * I5 多级缓存: L1 Caffeine(30s) → L2 Redis(可配置 TTL) → DB
 * 写入方更新数据后调用 invalidate(domain); 单机部署假定, 多实例时其他节点 L1 最多滞后 30s
 */
public interface TwoLevelCacheService {

    String DOMAIN_QUESTIONS = "questions";
    String DOMAIN_CATEGORIES = "categories";
    String DOMAIN_TAGS = "tags";

    <T> T get(String domain, String suffix, Duration ttl,
              TypeReference<T> type, Supplier<T> loader);

    /** 版本号自增 + 本地缓存清空; 旧 Redis key 靠 TTL 自然过期 */
    void invalidate(String domain);
}
