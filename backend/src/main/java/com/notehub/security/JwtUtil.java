package com.notehub.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

/**
 * JWT 工具 (jjwt 0.12 新 API)
 * HS256 签名; subject 存 userId; 过期时间由 jwt.expiration(秒) 控制
 */
@Log4j2
@Component
public class JwtUtil {

    private static final int MIN_SECRET_BYTES = 32; // HS256 要求 ≥ 32 字节

    private final SecretKey key;
    private final long expirationSeconds;

    public JwtUtil(@Value("${jwt.secret}") String secret,
                   @Value("${jwt.expiration-seconds:2592000}") long expirationSeconds) {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "jwt.secret 未配置或长度不足 " + MIN_SECRET_BYTES + " 字节, 请在 application-local.yaml 中配置");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationSeconds = expirationSeconds;
    }

    /** 为指定用户签发 token */
    public String generateToken(Long userId) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(expirationSeconds)))
                .signWith(key)
                .compact();
    }

    /**
     * 解析并校验 token
     * @return userId; 无效/过期返回 null (调用方按未登录处理, 不抛异常)
     */
    public Long parseUserId(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        try {
            Claims claims = Jwts.parser().verifyWith(key).build()
                    .parseSignedClaims(token).getPayload();
            return Long.valueOf(claims.getSubject());
        } catch (ExpiredJwtException e) {
            log.debug("[JWT] token 已过期");
            return null;
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("[JWT] token 无效: {}", e.getMessage());
            return null;
        }
    }
}
