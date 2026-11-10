package com.notehub;

import com.fasterxml.jackson.core.type.TypeReference;
import com.notehub.service.TwoLevelCacheService;

import java.time.Duration;
import java.util.function.Supplier;

/**
 * 单测用直通缓存: 不做缓存, 直接回源, 便于断言业务 SQL 调用
 */
public final class TestCaches {

    private TestCaches() {
    }

    public static TwoLevelCacheService passThrough() {
        return new TwoLevelCacheService() {
            @Override
            public <T> T get(String domain, String suffix, Duration ttl,
                             TypeReference<T> type, Supplier<T> loader) {
                return loader.get();
            }

            @Override
            public void invalidate(String domain) {
                // no-op
            }
        };
    }
}
