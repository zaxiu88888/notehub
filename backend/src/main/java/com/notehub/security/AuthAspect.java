package com.notehub.security;

import com.notehub.common.BusinessException;
import com.notehub.common.ErrorCode;
import com.notehub.common.RequestContext;
import lombok.extern.log4j.Log4j2;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/**
 * 鉴权切面: 环绕校验 @NeedLogin / @NeedAdmin
 * 未通过统一抛 BusinessException, 由 GlobalExceptionHandler 转成 {code,message};
 * 不直接返回 ApiResponse —— 否则返回 ResponseEntity 的接口会发生类型转换失败
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
            throw BusinessException.of(ErrorCode.UNAUTHORIZED);
        }
        return joinPoint.proceed();
    }

    @Around("@annotation(needAdmin)")
    public Object aroundNeedAdmin(ProceedingJoinPoint joinPoint, NeedAdmin needAdmin) throws Throwable {
        if (!requestScope.isLogin()) {
            log.warn("[鉴权] 未登录访问管理接口: {}", joinPoint.getSignature().toShortString());
            throw BusinessException.of(ErrorCode.UNAUTHORIZED);
        }
        if (!requestScope.isAdmin()) {
            log.warn("[鉴权] 非管理员访问管理接口: user={}, api={}",
                    requestScope.getUserId(), joinPoint.getSignature().toShortString());
            throw BusinessException.of(ErrorCode.ADMIN_REQUIRED);
        }
        return joinPoint.proceed();
    }
}
