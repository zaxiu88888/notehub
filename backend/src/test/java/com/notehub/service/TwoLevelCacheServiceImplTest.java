package com.notehub.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.notehub.service.impl.TwoLevelCacheServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TwoLevelCacheServiceImplTest {

    private RedisService redisService;
    private TwoLevelCacheServiceImpl cache;
    private final TypeReference<List<String>> type = new TypeReference<>() {
    };

    @BeforeEach
    void setUp() {
        redisService = mock(RedisService.class);
        cache = new TwoLevelCacheServiceImpl(redisService, new ObjectMapper());
    }

    @Test
    @DisplayName("L1 命中: 回源一次, 第二次不查 Redis")
    void localHitSkipsRedisAndLoader() {
        when(redisService.get(anyString())).thenReturn(null);
        AtomicInteger loads = new AtomicInteger();

        assertEquals(List.of("a"), cache.get("tags", "popular:20", Duration.ofMinutes(5),
                type, () -> { loads.incrementAndGet(); return List.of("a"); }));
        assertEquals(List.of("a"), cache.get("tags", "popular:20", Duration.ofMinutes(5),
                type, () -> { loads.incrementAndGet(); return List.of("a"); }));

        assertEquals(1, loads.get());
        verify(redisService, times(1)).get(anyString());
    }

    @Test
    @DisplayName("L2 命中: 反序列化后回填 L1, 不查库")
    void redisHitSkipsLoader() throws Exception {
        String json = new ObjectMapper().writeValueAsString(List.of("a", "b"));
        when(redisService.get(eq("cache:tags:v0:popular:20"))).thenReturn(json);
        AtomicInteger loads = new AtomicInteger();

        List<String> value = cache.get("tags", "popular:20", Duration.ofMinutes(5),
                type, () -> { loads.incrementAndGet(); return List.of(); });

        assertEquals(List.of("a", "b"), value);
        assertEquals(0, loads.get());
        verify(redisService, never()).set(anyString(), anyString(), any(Duration.class));
    }

    @Test
    @DisplayName("失效: 版本号递增, 旧缓存不再命中")
    void invalidateBumpsVersion() {
        when(redisService.get(anyString())).thenReturn(null);
        AtomicInteger loads = new AtomicInteger();
        cache.get("tags", "popular:20", Duration.ofMinutes(5),
                type, () -> { loads.incrementAndGet(); return List.of("old"); });

        cache.invalidate("tags");
        List<String> fresh = cache.get("tags", "popular:20", Duration.ofMinutes(5),
                type, () -> { loads.incrementAndGet(); return List.of("new"); });

        assertEquals(List.of("new"), fresh);
        assertEquals(2, loads.get());
    }

    @Test
    @DisplayName("Redis 故障: 降级回源, 不影响结果")
    void redisFailureFallsBackToLoader() {
        when(redisService.get(anyString())).thenThrow(new IllegalStateException("redis down"));
        AtomicInteger loads = new AtomicInteger();

        List<String> value = cache.get("tags", "popular:20", Duration.ofMinutes(5),
                type, () -> { loads.incrementAndGet(); return List.of("a"); });

        assertEquals(List.of("a"), value);
        assertEquals(1, loads.get());
    }
}
