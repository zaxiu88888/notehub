package com.notehub.service;

import java.time.Duration;

/**
 * 固定窗口限流
 */
public interface RateLimitService {

    /** @return true=放行; false=超过 limit 次/窗口 */
    boolean tryAcquire(String key, int limit, Duration window);
}
