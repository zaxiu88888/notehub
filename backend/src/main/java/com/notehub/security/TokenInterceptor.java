package com.notehub.security;

import com.notehub.common.RequestContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 认证拦截器: 解析 Authorization: Bearer <jwt> → 写入 RequestContext
 * 只负责"识别你是谁", 不做拦截(登录态校验由 @NeedLogin/@NeedAdmin 决定)
 * 游客访问匿名接口时 RequestContext 保持匿名 → 接口返回"我"维度的默认值
 */
@Log4j2
@Component
public class TokenInterceptor implements HandlerInterceptor {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtUtil jwtUtil;
    private final RequestContext requestScope;

    public TokenInterceptor(JwtUtil jwtUtil, RequestContext requestScope) {
        this.jwtUtil = jwtUtil;
        this.requestScope = requestScope;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        requestScope.markAnonymous();

        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            return true;
        }
        String token = authorization.substring(BEARER_PREFIX.length()).trim();
        Long userId = jwtUtil.parseUserId(token);
        if (userId != null) {
            // MVP 权限模型: JWT 中只存 userId, isAdmin 以库内实时状态为准(M1 用户模块落地)
            // 当前阶段先按普通用户识别, 管理员判定接入 users 表后由 UserService 覆盖 markLogin
            requestScope.markLogin(userId, false);
        }
        return true;
    }
}
