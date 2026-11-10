package com.notehub.service.impl;

import com.notehub.service.RateLimitService;
import com.notehub.service.RedisService;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Log4j2
@Service
public class RateLimitServiceImpl implements RateLimitService {

    private final RedisService redisService;

    public RateLimitServiceImpl(RedisService redisService) {
        this.redisService = redisService;
    }

    @Override
    public boolean tryAcquire(String key, int limit, Duration window) {
        try {
            long count = redisService.incrementWithTtl(key, window);
            return count <= limit;
        } catch (Exception e) {
            // Redis 故障时 fail-open, 避免限流组件拖垮主流程
            log.warn("[限流] Redis 不可用, 降级放行: key={}, err={}", key, e.getMessage());
            return true;
        }
    }
}
