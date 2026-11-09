package com.notehub.common;

import lombok.extern.log4j.Log4j2;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.stereotype.Component;

/**
 * 死锁重试切面
 * 顺序: 必须位于 @Transactional 之外(Order 值小于事务顾问的 LOWEST_PRECEDENCE),
 * 否则重试发生在已回滚的事务内部, 无法真正重试。
 */
@Log4j2
@Aspect
@Component
@Order(0)
public class DeadlockRetryAspect {

    @Around("@annotation(retryOnDeadlock)")
    public Object retry(ProceedingJoinPoint joinPoint, RetryOnDeadlock retryOnDeadlock) throws Throwable {
        int maxAttempts = Math.max(1, retryOnDeadlock.maxAttempts());
        long backoff = Math.max(0, retryOnDeadlock.backoffMillis());

        for (int attempt = 1; ; attempt++) {
            try {
                return joinPoint.proceed();
            } catch (Throwable e) {
                if (!isRetryable(e) || attempt >= maxAttempts) {
                    throw e;
                }
                log.info("[并发] 死锁后重试 {}/{}: {}", attempt, maxAttempts - 1,
                        joinPoint.getSignature().toShortString());
                if (backoff > 0) {
                    try {
                        Thread.sleep(backoff * attempt);
                    } catch (InterruptedException interrupted) {
                        Thread.currentThread().interrupt();
                        throw e;
                    }
                }
            }
        }
    }

    private boolean isRetryable(Throwable e) {
        Throwable current = e;
        while (current != null) {
            if (current instanceof PessimisticLockingFailureException) {
                return true; // 含 DeadlockLoserDataAccessException / CannotAcquireLockException
            }
            current = current.getCause();
        }
        return false;
    }
}
