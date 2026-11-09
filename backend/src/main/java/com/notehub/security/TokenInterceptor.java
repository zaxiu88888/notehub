package com.notehub.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.notehub.common.ApiResponse;
import com.notehub.common.ErrorCode;
import com.notehub.common.RequestContext;
import com.notehub.mapper.UserMapper;
import com.notehub.model.entity.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 认证拦截器: 解析 Authorization: Bearer <jwt> → 查 users 表 → 写入 RequestContext
 * - 识别身份, 并按路径对 /api/admin/** 做**前置**管理员校验
 *   (若交给 @NeedAdmin AOP, 参数绑定先执行, 非法请求体会先返回 40001 而不是 40102)
 * - isAdmin 以库内实时状态为准(降权即时生效); 封禁用户按匿名处理
 */
@Log4j2
@Component
public class TokenInterceptor implements HandlerInterceptor {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String ADMIN_PATH_PREFIX = "/api/admin/";

    private final JwtUtil jwtUtil;
    private final UserMapper userMapper;
    private final RequestContext requestContext;
    private final ObjectMapper objectMapper;

    public TokenInterceptor(JwtUtil jwtUtil, UserMapper userMapper,
                            RequestContext requestContext, ObjectMapper objectMapper) {
        this.jwtUtil = jwtUtil;
        this.userMapper = userMapper;
        this.requestContext = requestContext;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        requestContext.markAnonymous();

        String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.startsWith(BEARER_PREFIX)) {
            String token = authorization.substring(BEARER_PREFIX.length()).trim();
            Long userId = jwtUtil.parseUserId(token);
            if (userId != null) {
                User user = userMapper.selectById(userId);
                if (user != null && (user.getIsBanned() == null || user.getIsBanned() != 1)) {
                    boolean admin = user.getIsAdmin() != null && user.getIsAdmin() == 1;
                    requestContext.markLogin(userId, admin);
                }
            }
        }

        // 管理接口前置守卫: 未登录/非管理员直接 40102, 不再进入参数绑定
        String path = request.getRequestURI();
        if (path.startsWith(ADMIN_PATH_PREFIX) && !requestContext.isAdmin()) {
            writeUnauthorized(response, requestContext.isLogin()
                    ? "需要管理员权限" : "用户未登录",
                    requestContext.isLogin() ? ErrorCode.ADMIN_REQUIRED : ErrorCode.UNAUTHORIZED);
            return false;
        }
        return true;
    }

    private void writeUnauthorized(HttpServletResponse response, String message, ErrorCode code)
            throws Exception {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(ApiResponse.error(code, message)));
    }
}
