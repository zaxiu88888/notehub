package com.notehub.security;

import com.notehub.common.BusinessException;
import com.notehub.common.ErrorCode;
import com.notehub.common.RequestContext;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 鉴权切面必须抛业务异常, 不能直接返回 ApiResponse:
 * 返回 ResponseEntity 的接口(如笔记导出)会因返回类型不匹配变成 500
 */
class AuthAspectTest {

    private RequestContext requestContext;
    private ProceedingJoinPoint joinPoint;
    private AuthAspect aspect;

    @BeforeEach
    void setUp() throws Throwable {
        requestContext = mock(RequestContext.class);
        joinPoint = mock(ProceedingJoinPoint.class);
        Signature signature = mock(Signature.class);
        when(signature.toShortString()).thenReturn("NoteController.download()");
        when(joinPoint.getSignature()).thenReturn(signature);
        when(joinPoint.proceed()).thenReturn("ok");
        aspect = new AuthAspect(requestContext);
    }

    @Test
    @DisplayName("未登录访问 @NeedLogin → 抛 40101 且不执行方法")
    void needLoginRejectsAnonymous() throws Throwable {
        when(requestContext.isLogin()).thenReturn(false);

        BusinessException e = assertThrows(BusinessException.class,
                () -> aspect.aroundNeedLogin(joinPoint, null));

        assertEquals(ErrorCode.UNAUTHORIZED, e.getErrorCode());
        verify(joinPoint, never()).proceed();
    }

    @Test
    @DisplayName("已登录访问 @NeedLogin → 放行")
    void needLoginAllowsLoggedIn() throws Throwable {
        when(requestContext.isLogin()).thenReturn(true);

        assertEquals("ok", aspect.aroundNeedLogin(joinPoint, null));
        verify(joinPoint).proceed();
    }

    @Test
    @DisplayName("普通用户访问 @NeedAdmin → 抛 40102")
    void needAdminRejectsNormalUser() throws Throwable {
        when(requestContext.isLogin()).thenReturn(true);
        when(requestContext.isAdmin()).thenReturn(false);

        BusinessException e = assertThrows(BusinessException.class,
                () -> aspect.aroundNeedAdmin(joinPoint, null));

        assertEquals(ErrorCode.ADMIN_REQUIRED, e.getErrorCode());
        verify(joinPoint, never()).proceed();
    }

    @Test
    @DisplayName("未登录访问 @NeedAdmin → 抛 40101")
    void needAdminRejectsAnonymous() throws Throwable {
        when(requestContext.isLogin()).thenReturn(false);

        BusinessException e = assertThrows(BusinessException.class,
                () -> aspect.aroundNeedAdmin(joinPoint, null));

        assertEquals(ErrorCode.UNAUTHORIZED, e.getErrorCode());
        verify(joinPoint, never()).proceed();
    }

    @Test
    @DisplayName("管理员访问 @NeedAdmin → 放行")
    void needAdminAllowsAdmin() throws Throwable {
        when(requestContext.isLogin()).thenReturn(true);
        when(requestContext.isAdmin()).thenReturn(true);

        assertEquals("ok", aspect.aroundNeedAdmin(joinPoint, null));
        verify(joinPoint).proceed();
    }
}
