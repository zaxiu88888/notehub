package com.notehub.service;

import com.notehub.service.impl.RateLimitServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RateLimitServiceImplTest {

    private RedisService redisService;
    private RateLimitServiceImpl service;

    @BeforeEach
    void setUp() {
        redisService = mock(RedisService.class);
        service = new RateLimitServiceImpl(redisService);
    }

    @Test
    @DisplayName("固定窗口: 前 limit 次放行, 第 limit+1 次拒绝")
    void allowsUpToLimit() {
        when(redisService.incrementWithTtl(anyString(), any(Duration.class)))
                .thenReturn(1L, 2L, 3L, 4L);

        assertTrue(service.tryAcquire("rate:login:u1", 3, Duration.ofMinutes(1)));
        assertTrue(service.tryAcquire("rate:login:u1", 3, Duration.ofMinutes(1)));
        assertTrue(service.tryAcquire("rate:login:u1", 3, Duration.ofMinutes(1)));
        assertFalse(service.tryAcquire("rate:login:u1", 3, Duration.ofMinutes(1)));
    }

    @Test
    @DisplayName("Redis 故障: fail-open 放行")
    void failsOpenOnRedisError() {
        when(redisService.incrementWithTtl(anyString(), any(Duration.class)))
                .thenThrow(new IllegalStateException("redis down"));

        assertTrue(service.tryAcquire("rate:login:u1", 1, Duration.ofMinutes(1)));
    }
}
