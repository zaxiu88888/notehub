package com.notehub.security;

import lombok.extern.log4j.Log4j2;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * STOMP 鉴权:
 * - CONNECT: 解析 Authorization(Bearer) / token 原生头, 校验 JWT 并绑定 Principal(userId)
 * - SUBSCRIBE: 只允许 /user/queue/** (个人队列), 防止订阅他人消息
 * - SEND: 本项目不需要客户端上行消息, 一律拒绝
 */
@Log4j2
@Component
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtUtil jwtUtil;

    public StompAuthChannelInterceptor(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() == null) {
            return message;
        }
        StompCommand command = accessor.getCommand();
        if (StompCommand.CONNECT.equals(command)) {
            Long userId = jwtUtil.parseUserId(extractToken(accessor));
            if (userId == null || userId <= 0) {
                log.debug("[WS] 拒绝未认证连接");
                throw new MessagingException("WebSocket 连接未认证");
            }
            accessor.setUser(new UsernamePasswordAuthenticationToken(
                    String.valueOf(userId), null, List.of()));
            log.debug("[WS] 认证通过: userId={}", userId);
        } else if (StompCommand.SUBSCRIBE.equals(command)) {
            String destination = accessor.getDestination();
            if (destination == null || !destination.startsWith("/user/queue/")) {
                throw new MessagingException("仅允许订阅个人消息队列");
            }
        } else if (StompCommand.SEND.equals(command)) {
            throw new MessagingException("不支持客户端发送消息");
        }
        return message;
    }

    private String extractToken(StompHeaderAccessor accessor) {
        String authorization = accessor.getFirstNativeHeader("Authorization");
        if (authorization != null && authorization.startsWith(BEARER_PREFIX)) {
            return authorization.substring(BEARER_PREFIX.length()).trim();
        }
        String token = accessor.getFirstNativeHeader("token");
        return token == null ? null : token.trim();
    }
}
