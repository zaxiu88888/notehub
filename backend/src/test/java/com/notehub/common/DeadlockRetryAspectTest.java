package com.notehub.common;

import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.dao.DeadlockLoserDataAccessException;
import org.springframework.dao.DuplicateKeyException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DeadlockRetryAspectTest {

    private final DeadlockRetryAspect aspect = new DeadlockRetryAspect();

    private RetryOnDeadlock annotation(int attempts, long backoff) {
        RetryOnDeadlock a = Mockito.mock(RetryOnDeadlock.class);
        when(a.maxAttempts()).thenReturn(attempts);
        when(a.backoffMillis()).thenReturn(backoff);
        return a;
    }

    @Test
    @DisplayName("死锁两次后成功: 重试生效并返回结果")
    void retriesThenSucceeds() throws Throwable {
        ProceedingJoinPoint pjp = Mockito.mock(ProceedingJoinPoint.class);
        when(pjp.proceed())
                .thenThrow(new DeadlockLoserDataAccessException("d1", null))
                .thenThrow(new DeadlockLoserDataAccessException("d2", null))
                .thenReturn("ok");
        when(pjp.getSignature()).thenReturn(Mockito.mock(org.aspectj.lang.Signature.class));

        Object result = aspect.retry(pjp, annotation(3, 0));

        assertEquals("ok", result);
        verify(pjp, times(3)).proceed();
    }

    @Test
    @DisplayName("重试次数耗尽: 抛出原异常(由全局处理映射 40902)")
    void exhaustsAttempts() throws Throwable {
        ProceedingJoinPoint pjp = Mockito.mock(ProceedingJoinPoint.class);
        when(pjp.proceed()).thenThrow(new DeadlockLoserDataAccessException("always", null));
        when(pjp.getSignature()).thenReturn(Mockito.mock(org.aspectj.lang.Signature.class));

        assertThrows(DeadlockLoserDataAccessException.class, () -> aspect.retry(pjp, annotation(3, 0)));
        verify(pjp, times(3)).proceed();
    }

    @Test
    @DisplayName("非死锁异常不重试, 直接抛出")
    void nonRetryablePropagates() throws Throwable {
        ProceedingJoinPoint pjp = Mockito.mock(ProceedingJoinPoint.class);
        when(pjp.proceed()).thenThrow(new DuplicateKeyException("dup"));

        assertThrows(DuplicateKeyException.class, () -> aspect.retry(pjp, annotation(3, 0)));
        verify(pjp, times(1)).proceed();
    }
}
