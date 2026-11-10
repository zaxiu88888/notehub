package com.notehub.security;

import com.notehub.common.BusinessException;
import com.notehub.common.ErrorCode;
import com.notehub.common.RateLimit;
import com.notehub.common.RequestContext;
import com.notehub.service.RateLimitService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.context.request.RequestContextHolder;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RateLimitAspectTest {

    private RateLimitService rateLimitService;
    private RequestContext requestContext;
    private RateLimitAspect aspect;
    private ProceedingJoinPoint joinPoint;

    static class Dummy {
        @RateLimit(key = "login", limit = 10, windowSeconds = 60)
        public void limited() {
        }
    }

    @BeforeEach
    void setUp() throws Throwable {
        rateLimitService = mock(RateLimitService.class);
        requestContext = mock(RequestContext.class);
        aspect = new RateLimitAspect(rateLimitService, requestContext);
        joinPoint = mock(ProceedingJoinPoint.class);
        when(joinPoint.proceed()).thenReturn("ok");
        RequestContextHolder.resetRequestAttributes();
    }

    private RateLimit annotation() throws Exception {
        return Dummy.class.getMethod("limited").getAnnotation(RateLimit.class);
    }

    @Test
    @DisplayName("登录用户: 按 userId 维度, 放行时执行业务")
    void allowsWithUserKey() throws Throwable {
        when(requestContext.isLogin()).thenReturn(true);
        when(requestContext.getUserId()).thenReturn(7L);
        when(rateLimitService.tryAcquire(eq("rate:login:u7"), eq(10), any(Duration.class)))
                .thenReturn(true);

        Object result = aspect.around(joinPoint, annotation());

        assertEquals("ok", result);
        verify(joinPoint).proceed();
    }

    @Test
    @DisplayName("匿名: 按 IP 维度; 超限抛 42901 且不执行业务")
    void rejectsWithIpKey() throws Throwable {
        when(requestContext.isLogin()).thenReturn(false);
        when(rateLimitService.tryAcquire(eq("rate:login:ipunknown"), eq(10), any(Duration.class)))
                .thenReturn(false);

        BusinessException e = assertThrows(BusinessException.class,
                () -> aspect.around(joinPoint, annotation()));

        assertEquals(ErrorCode.TOO_MANY_REQUESTS, e.getErrorCode());
        verify(joinPoint, never()).proceed();
        verify(rateLimitService).tryAcquire(eq("rate:login:ipunknown"), eq(10), any(Duration.class));
    }

}
