package com.notehub.security;

import lombok.extern.log4j.Log4j2;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket 会话注册表:
 * - WebSocket 连接建立/关闭时维护 sessionId → WebSocketSession
 * - STOMP CONNECT 成功(SessionConnectedEvent)后绑定 userId → sessionId(支持多标签页)
 * - 封禁用户时主动关闭其全部会话, 弥补"只在 CONNECT 鉴权一次"的空档
 */
@Log4j2
@Component
public class WsSessionRegistry {

    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final Map<Long, Set<String>> userSessions = new ConcurrentHashMap<>();
    private final Map<String, Long> sessionOwners = new ConcurrentHashMap<>();

    public void registerSession(WebSocketSession session) {
        sessions.put(session.getId(), session);
        log.debug("[WS] 注册底层会话: sessionId={}", session.getId());
    }

    public void bindUser(Long userId, String sessionId) {
        if (userId == null || sessionId == null) {
            return;
        }
        Long previous = sessionOwners.put(sessionId, userId);
        if (previous != null && !previous.equals(userId)) {
            removeFromUser(previous, sessionId);
        }
        userSessions.computeIfAbsent(userId, key -> ConcurrentHashMap.newKeySet()).add(sessionId);
        log.debug("[WS] 绑定用户会话: userId={}, sessionId={}", userId, sessionId);
    }

    public void unregisterSession(String sessionId) {
        if (sessionId == null) {
            return;
        }
        sessions.remove(sessionId);
        Long userId = sessionOwners.remove(sessionId);
        if (userId != null) {
            removeFromUser(userId, sessionId);
        }
    }

    /** 封禁/删除用户时调用: 关闭该用户所有已建立的 WS 连接 */
    public void disconnectUser(Long userId) {
        if (userId == null) {
            return;
        }
        Set<String> sessionIds = userSessions.remove(userId);
        if (sessionIds == null || sessionIds.isEmpty()) {
            log.debug("[WS] 封禁用户无已注册会话: userId={}", userId);
            return;
        }
        for (String sessionId : sessionIds) {
            sessionOwners.remove(sessionId);
            WebSocketSession session = sessions.remove(sessionId);
            if (session == null) {
                continue;
            }
            try {
                if (session.isOpen()) {
                    session.close(CloseStatus.POLICY_VIOLATION.withReason("账号已被封禁"));
                }
            } catch (Exception e) {
                log.warn("[WS] 关闭封禁用户会话失败: userId={}, sessionId={}, err={}",
                        userId, sessionId, e.getMessage());
            }
        }
        log.info("[WS] 已断开封禁用户会话: userId={}, sessions={}", userId, sessionIds.size());
    }

    @EventListener
    public void onConnected(SessionConnectedEvent event) {
        Principal user = event.getUser();
        String sessionId = SimpMessageHeaderAccessor.getSessionId(event.getMessage().getHeaders());
        log.debug("[WS] SessionConnected: user={}, sessionId={}",
                user == null ? null : user.getName(), sessionId);
        if (user == null || user.getName() == null) {
            return;
        }
        try {
            bindUser(Long.valueOf(user.getName()), sessionId);
        } catch (NumberFormatException e) {
            log.warn("[WS] 无法解析 Principal: {}", user.getName());
        }
    }

    @EventListener
    public void onDisconnect(SessionDisconnectEvent event) {
        unregisterSession(event.getSessionId());
    }

    private void removeFromUser(Long userId, String sessionId) {
        userSessions.computeIfPresent(userId, (key, ids) -> {
            ids.remove(sessionId);
            return ids.isEmpty() ? null : ids;
        });
    }
}
