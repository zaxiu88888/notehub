package com.notehub.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.notehub.common.RequestContext;
import com.notehub.mapper.UserMapper;
import com.notehub.model.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 管理接口前置守卫: 必须在参数绑定之前返回 40102/40101
 */
class TokenInterceptorTest {

    private static final String SECRET = "unit-test-secret-0123456789-unit-test-secret-0123456789";

    private JwtUtil jwtUtil;
    private UserMapper userMapper;
    private TokenInterceptor interceptor;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil(SECRET, 3600);
        userMapper = mock(UserMapper.class);
        interceptor = new TokenInterceptor(jwtUtil, userMapper, new RequestContext(), new ObjectMapper());
    }

    private MockHttpServletRequest request(String uri, String token) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", uri);
        if (token != null) request.addHeader("Authorization", "Bearer " + token);
        return request;
    }

    @Test
    @DisplayName("匿名访问 /api/admin/** → 40102(先于参数绑定)")
    void anonymousAdminPathRejected() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean proceed = interceptor.preHandle(request("/api/admin/categories", null), response, new Object());

        assertFalse(proceed);
        assertTrue(response.getContentAsString().contains("\"code\":40101"));
    }

    @Test
    @DisplayName("普通用户访问 /api/admin/** → 40102")
    void normalUserAdminPathRejected() throws Exception {
        User demo = new User();
        demo.setUserId(2L);
        demo.setIsAdmin(0);
        demo.setIsBanned(0);
        when(userMapper.selectById(2L)).thenReturn(demo);
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean proceed = interceptor.preHandle(
                request("/api/admin/questions", jwtUtil.generateToken(2L)), response, new Object());

        assertFalse(proceed);
        assertTrue(response.getContentAsString().contains("\"code\":40102"));
    }

    @Test
    @DisplayName("管理员访问 /api/admin/** → 放行")
    void adminPathAllowed() throws Exception {
        User admin = new User();
        admin.setUserId(1L);
        admin.setIsAdmin(1);
        admin.setIsBanned(0);
        when(userMapper.selectById(1L)).thenReturn(admin);

        boolean proceed = interceptor.preHandle(
                request("/api/admin/questions", jwtUtil.generateToken(1L)), new MockHttpServletResponse(), new Object());

        assertTrue(proceed);
    }

    @Test
    @DisplayName("封禁用户 token 按匿名处理; 非 admin 路径放行")
    void bannedUserTreatedAnonymous() throws Exception {
        User banned = new User();
        banned.setUserId(5L);
        banned.setIsAdmin(1);
        banned.setIsBanned(1);
        when(userMapper.selectById(5L)).thenReturn(banned);
        MockHttpServletResponse response = new MockHttpServletResponse();

        // 普通接口: 放行但匿名
        assertTrue(interceptor.preHandle(request("/api/notes", jwtUtil.generateToken(5L)), response, new Object()));
        // 管理接口: 封禁后按匿名 → 40101
        MockHttpServletResponse adminResponse = new MockHttpServletResponse();
        assertFalse(interceptor.preHandle(request("/api/admin/users", jwtUtil.generateToken(5L)), adminResponse, new Object()));
        assertEquals(true, adminResponse.getContentAsString().contains("\"code\":40101"));
    }
}
