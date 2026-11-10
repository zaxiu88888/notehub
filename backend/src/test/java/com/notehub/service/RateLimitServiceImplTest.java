package com.notehub.service;

import com.notehub.service.impl.RateLimitServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
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
    @DisplayName("滑动窗口: 放行/拒绝直接透传 Redis 结果")
    void delegatesToSlidingWindow() {
        when(redisService.slidingWindowAcquire("rate:login:u1", 3, Duration.ofMinutes(1)))
                .thenReturn(true, true, true, false);

        assertTrue(service.tryAcquire("rate:login:u1", 3, Duration.ofMinutes(1)));
        assertTrue(service.tryAcquire("rate:login:u1", 3, Duration.ofMinutes(1)));
        assertTrue(service.tryAcquire("rate:login:u1", 3, Duration.ofMinutes(1)));
        assertFalse(service.tryAcquire("rate:login:u1", 3, Duration.ofMinutes(1)));
    }

    @Test
    @DisplayName("Redis 故障: fail-open 放行")
    void failsOpenOnRedisError() {
        when(redisService.slidingWindowAcquire(anyString(), anyInt(), any(Duration.class)))
                .thenThrow(new IllegalStateException("redis down"));

        assertTrue(service.tryAcquire("rate:login:u1", 1, Duration.ofMinutes(1)));
    }
}
