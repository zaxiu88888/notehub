package com.notehub.security;

import com.notehub.common.BusinessException;
import com.notehub.common.ErrorCode;
import com.notehub.common.RateLimit;
import com.notehub.common.RequestContext;
import com.notehub.service.RateLimitService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.log4j.Log4j2;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Duration;

/**
 * 限流切面: key = rate:{scope}:{userId | ip}
 * 登录态按用户维度, 匿名按客户端 IP; 超限抛 42901(由全局异常处理统一返回)
 */
@Log4j2
@Aspect
@Component
public class RateLimitAspect {

    private final RateLimitService rateLimitService;
    private final RequestContext requestContext;

    public RateLimitAspect(RateLimitService rateLimitService, RequestContext requestContext) {
        this.rateLimitService = rateLimitService;
        this.requestContext = requestContext;
    }

    @Around("@annotation(rateLimit)")
    public Object around(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        String scope = rateLimit.key().isBlank()
                ? joinPoint.getSignature().toShortString() : rateLimit.key();
        String identity = requestContext.isLogin()
                ? "u" + requestContext.getUserId() : "ip" + clientIp();
        String key = "rate:" + scope + ":" + identity;
        Duration window = Duration.ofSeconds(rateLimit.windowSeconds());

        if (!rateLimitService.tryAcquire(key, rateLimit.limit(), window)) {
            log.warn("[限流] 触发: key={}, limit={}/{}s", key, rateLimit.limit(),
                    rateLimit.windowSeconds());
            throw BusinessException.of(ErrorCode.TOO_MANY_REQUESTS);
        }
        return joinPoint.proceed();
    }

    private String clientIp() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes instanceof ServletRequestAttributes servletAttributes) {
            HttpServletRequest request = servletAttributes.getRequest();
            String forwarded = request.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                return forwarded.split(",")[0].trim();
            }
            return request.getRemoteAddr();
        }
        return "unknown";
    }
}
