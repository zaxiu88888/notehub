package com.notehub.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApiResponseTest {

    @Test
    @DisplayName("ok(data) 应返回 200/success 并携带数据")
    void okShouldCarryData() {
        ApiResponse<String> resp = ApiResponse.ok("data-x");
        assertTrue(resp.isSuccess());
        assertEquals(200, resp.getCode());
        assertEquals("success", resp.getMessage());
        assertEquals("data-x", resp.getData());
    }

    @Test
    @DisplayName("ok(message, data) 重载应同时携带自定义文案与数据")
    void okWithMessageShouldCarryBoth() {
        ApiResponse<String> resp = ApiResponse.ok("hello", "data-x");
        assertTrue(resp.isSuccess());
        assertEquals("hello", resp.getMessage());
        assertEquals("data-x", resp.getData());
    }

    @Test
    @DisplayName("ok() 无数据时 data 应为 null")
    void okWithoutDataShouldBeNull() {
        ApiResponse<Void> resp = ApiResponse.ok();
        assertTrue(resp.isSuccess());
        assertNull(resp.getData());
    }

    @Test
    @DisplayName("error(ErrorCode) 应透出错误码与默认文案")
    void errorShouldUseErrorCode() {
        ApiResponse<Void> resp = ApiResponse.error(ErrorCode.UNAUTHORIZED);
        assertFalse(resp.isSuccess());
        assertEquals(40101, resp.getCode());
        assertEquals("用户未登录", resp.getMessage());
    }

    @Test
    @DisplayName("error(ErrorCode, message) 应允许覆盖文案但保留 code")
    void errorShouldAllowCustomMessage() {
        ApiResponse<Void> resp = ApiResponse.error(ErrorCode.BAD_PARAM, "title 不能为空");
        assertEquals(40001, resp.getCode());
        assertEquals("title 不能为空", resp.getMessage());
    }

    @Test
    @DisplayName("BusinessException 应携带 ErrorCode")
    void businessExceptionShouldCarryErrorCode() {
        BusinessException e = BusinessException.of(ErrorCode.NOT_FOUND);
        assertEquals(ErrorCode.NOT_FOUND, e.getErrorCode());
        assertEquals(ErrorCode.NOT_FOUND.getMessage(), e.getMessage());
    }

    @Test
    @DisplayName("RequestContext: 匿名时 requireUserId 抛未登录; 登录后返回 userId")
    void requestContextLoginFlow() {
        RequestContext scope = new RequestContext();
        assertFalse(scope.isLogin());

        scope.markAnonymous();
        assertFalse(scope.isLogin());

        scope.markLogin(42L, false);
        assertTrue(scope.isLogin());
        assertFalse(scope.isAdmin());
        assertEquals(42L, scope.requireUserId());

        scope.markLogin(1L, true);
        assertTrue(scope.isAdmin());
    }
}
