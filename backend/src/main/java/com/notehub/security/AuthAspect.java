package com.notehub.security;

import com.notehub.common.ApiResponse;
import com.notehub.common.ErrorCode;
import com.notehub.common.RequestContext;
import lombok.extern.log4j.Log4j2;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/**
 * 鉴权切面: 环绕校验 @NeedLogin / @NeedAdmin
 * 未通过直接返回统一错误响应, 不进入业务方法
 */
@Log4j2
@Aspect
@Component
public class AuthAspect {

    private final RequestContext requestScope;

    public AuthAspect(RequestContext requestScope) {
        this.requestScope = requestScope;
    }

    @Around("@annotation(needLogin)")
    public Object aroundNeedLogin(ProceedingJoinPoint joinPoint, NeedLogin needLogin) throws Throwable {
        if (!requestScope.isLogin()) {
            log.info("[鉴权] 未登录访问需登录接口: {}", joinPoint.getSignature().toShortString());
            return ApiResponse.error(ErrorCode.UNAUTHORIZED);
        }
        return joinPoint.proceed();
    }

    @Around("@annotation(needAdmin)")
    public Object aroundNeedAdmin(ProceedingJoinPoint joinPoint, NeedAdmin needAdmin) throws Throwable {
        if (!requestScope.isLogin()) {
            log.warn("[鉴权] 未登录访问管理接口: {}", joinPoint.getSignature().toShortString());
            return ApiResponse.error(ErrorCode.UNAUTHORIZED);
        }
        if (!requestScope.isAdmin()) {
            log.warn("[鉴权] 非管理员访问管理接口: user={}, api={}",
                    requestScope.getUserId(), joinPoint.getSignature().toShortString());
            return ApiResponse.error(ErrorCode.ADMIN_REQUIRED);
        }
        return joinPoint.proceed();
    }
}
