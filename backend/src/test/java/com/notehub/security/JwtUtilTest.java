package com.notehub.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 纯单元测试: 不起 Spring 上下文, 直接构造 JwtUtil
 */
class JwtUtilTest {

    private static final String SECRET_48B = "unit-test-secret-0123456789-unit-test-secret-0123456789";
    private static final long TTL_1H = 3600;

    @Test
    @DisplayName("签发后可解析出同一 userId")
    void generateThenParse() {
        JwtUtil jwtUtil = new JwtUtil(SECRET_48B, TTL_1H);
        String token = jwtUtil.generateToken(9527L);
        assertNotNull(token);
        assertEquals(9527L, jwtUtil.parseUserId(token));
    }

    @Test
    @DisplayName("过期 token 解析应返回 null 而非抛异常")
    void expiredTokenReturnsNull() {
        JwtUtil jwtUtil = new JwtUtil(SECRET_48B, -1); // 已过期
        String token = jwtUtil.generateToken(1L);
        assertNull(jwtUtil.parseUserId(token));
    }

    @Test
    @DisplayName("被篡改的 token 解析应返回 null")
    void tamperedTokenReturnsNull() {
        JwtUtil jwtUtil = new JwtUtil(SECRET_48B, TTL_1H);
        String token = jwtUtil.generateToken(1L);
        String tampered = token.substring(0, token.length() - 4) + "aaaa";
        assertNull(jwtUtil.parseUserId(tampered));
    }

    @Test
    @DisplayName("另一密钥签发的 token 解析应返回 null")
    void foreignKeyTokenReturnsNull() {
        JwtUtil issuer = new JwtUtil(SECRET_48B, TTL_1H);
        JwtUtil verifier = new JwtUtil(SECRET_48B + "-different-key-part2", TTL_1H);
        String token = issuer.generateToken(1L);
        assertNull(verifier.parseUserId(token));
    }

    @Test
    @DisplayName("密钥不足 32 字节应拒绝构造")
    void shortSecretShouldThrow() {
        assertThrows(IllegalStateException.class, () -> new JwtUtil("short-secret", TTL_1H));
    }

    @Test
    @DisplayName("null/空 token 解析应返回 null")
    void blankTokenReturnsNull() {
        JwtUtil jwtUtil = new JwtUtil(SECRET_48B, TTL_1H);
        assertNull(jwtUtil.parseUserId(null));
        assertNull(jwtUtil.parseUserId(""));
    }
}
