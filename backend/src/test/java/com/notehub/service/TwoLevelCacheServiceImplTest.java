package com.notehub.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.notehub.service.impl.TwoLevelCacheServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doAnswer;

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
        verify(redisService, times(2)).get("cache:version:tags");
        verify(redisService, times(1)).get("cache:tags:v0:popular:20");
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
    @DisplayName("失效: Redis 版本号递增, 旧缓存不再命中")
    void invalidateBumpsVersion() {
        when(redisService.get("cache:version:tags")).thenReturn("0", "1");
        when(redisService.increment("cache:version:tags")).thenReturn(1L);
        AtomicInteger loads = new AtomicInteger();
        cache.get("tags", "popular:20", Duration.ofMinutes(5),
                type, () -> { loads.incrementAndGet(); return List.of("old"); });

        cache.invalidate("tags");
        List<String> fresh = cache.get("tags", "popular:20", Duration.ofMinutes(5),
                type, () -> { loads.incrementAndGet(); return List.of("new"); });

        assertEquals(List.of("new"), fresh);
        assertEquals(2, loads.get());
        verify(redisService).get("cache:tags:v1:popular:20");
    }

    @Test
    @DisplayName("跨实例失效: 其他实例 L1 持有旧值, Redis 版本变化后立即回源")
    void invalidateSharedAcrossInstances() {
        AtomicLong sharedVersion = new AtomicLong(0);
        Map<String, String> l2 = new HashMap<>();
        when(redisService.get("cache:version:categories")).thenAnswer(inv -> String.valueOf(sharedVersion.get()));
        when(redisService.increment("cache:version:categories")).thenAnswer(inv -> sharedVersion.incrementAndGet());
        when(redisService.get(argThat(k -> k != null && k.startsWith("cache:categories:v"))))
                .thenAnswer(inv -> l2.get(inv.getArgument(0)));
        doAnswer(inv -> {
            l2.put(inv.getArgument(0), inv.getArgument(1));
            return null;
        }).when(redisService).set(anyString(), anyString(), any(Duration.class));

        TwoLevelCacheServiceImpl nodeA = new TwoLevelCacheServiceImpl(redisService, new ObjectMapper());
        TwoLevelCacheServiceImpl nodeB = new TwoLevelCacheServiceImpl(redisService, new ObjectMapper());
        AtomicInteger loads = new AtomicInteger();

        List<String> a = nodeA.get("categories", "tree", Duration.ofMinutes(5), type,
                () -> { loads.incrementAndGet(); return List.of("old"); });
        List<String> b = nodeB.get("categories", "tree", Duration.ofMinutes(5), type,
                () -> { loads.incrementAndGet(); return List.of("old"); });
        assertEquals(List.of("old"), a);
        assertEquals(List.of("old"), b);
        assertEquals(1, loads.get()); // B 命中 L2 缓存

        // A 节点写入后失效; B 节点 L1 仍在, 但新版本 key 使其立即回源
        nodeA.invalidate("categories");
        List<String> fresh = nodeB.get("categories", "tree", Duration.ofMinutes(5), type,
                () -> { loads.incrementAndGet(); return List.of("new"); });

        assertEquals(List.of("new"), fresh);
        assertEquals(2, loads.get());
    }

    @Test
    @DisplayName("击穿保护: 同一 key 并发未命中只回源一次")
    void concurrentMissLoadsOnce() throws Exception {
        when(redisService.get(anyString())).thenReturn(null);
        AtomicInteger loads = new AtomicInteger();
        int threads = 8;
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                try {
                    start.await();
                    cache.get("questions", "hot", Duration.ofMinutes(2), type, () -> {
                        loads.incrementAndGet();
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                        return List.of("q1");
                    });
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
        }
        start.countDown();
        done.await(5, TimeUnit.SECONDS);
        pool.shutdownNow();

        assertEquals(1, loads.get());
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
