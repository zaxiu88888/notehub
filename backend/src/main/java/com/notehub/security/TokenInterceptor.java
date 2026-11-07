package com.notehub.security;

import com.notehub.common.RequestContext;
import com.notehub.mapper.UserMapper;
import com.notehub.model.entity.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 认证拦截器: 解析 Authorization: Bearer <jwt> → 查 users 表 → 写入 RequestContext
 * - 只负责"识别你是谁", 不做拦截(登录态校验由 @NeedLogin/@NeedAdmin 决定)
 * - isAdmin 以库内实时状态为准(降权即时生效); 封禁用户按匿名处理
 * - 游客访问匿名接口时 RequestContext 保持匿名 → 接口返回"我"维度的默认值
 */
@Log4j2
@Component
public class TokenInterceptor implements HandlerInterceptor {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtUtil jwtUtil;
    private final UserMapper userMapper;
    private final RequestContext requestContext;

    public TokenInterceptor(JwtUtil jwtUtil, UserMapper userMapper, RequestContext requestContext) {
        this.jwtUtil = jwtUtil;
        this.userMapper = userMapper;
        this.requestContext = requestContext;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        requestContext.markAnonymous();

        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            return true;
        }
        String token = authorization.substring(BEARER_PREFIX.length()).trim();
        Long userId = jwtUtil.parseUserId(token);
        if (userId == null) {
            return true;
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            log.debug("[认证] token 对应用户不存在: {}", userId);
            return true;
        }
        if (user.getIsBanned() != null && user.getIsBanned() == 1) {
            log.debug("[认证] 封禁用户访问, 按匿名处理: {}", userId);
            return true;
        }
        boolean admin = user.getIsAdmin() != null && user.getIsAdmin() == 1;
        requestContext.markLogin(userId, admin);
        return true;
    }
}
