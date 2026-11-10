package com.notehub.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WsSessionRegistryTest {

    private WsSessionRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new WsSessionRegistry();
    }

    private WebSocketSession session(String id) throws Exception {
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getId()).thenReturn(id);
        when(session.isOpen()).thenReturn(true);
        return session;
    }

    @Test
    @DisplayName("封禁用户: 关闭其全部标签页会话, 且只关闭一次")
    void disconnectUserClosesAllSessions() throws Exception {
        WebSocketSession tab1 = session("s1");
        WebSocketSession tab2 = session("s2");
        registry.registerSession(tab1);
        registry.registerSession(tab2);
        registry.bindUser(7L, "s1");
        registry.bindUser(7L, "s2");

        registry.disconnectUser(7L);

        verify(tab1).close(argThat(status -> status.getCode() == CloseStatus.POLICY_VIOLATION.getCode()));
        verify(tab2).close(argThat(status -> status.getCode() == CloseStatus.POLICY_VIOLATION.getCode()));

        // 再封禁一次不应重复 close(注册表已清空)
        registry.disconnectUser(7L);
        verify(tab1, times(1)).close(argThat(status -> status.getCode() == 1008));
        verify(tab2, times(1)).close(argThat(status -> status.getCode() == 1008));
    }

    @Test
    @DisplayName("连接关闭后清理注册表, 后续封禁不再操作旧会话")
    void unregisterRemovesSession() throws Exception {
        WebSocketSession tab = session("s1");
        registry.registerSession(tab);
        registry.bindUser(7L, "s1");

        registry.unregisterSession("s1");
        registry.disconnectUser(7L);

        verify(tab, never()).close(argThat(status -> true));
    }

    @Test
    @DisplayName("封禁无连接用户: 不抛异常")
    void disconnectUnknownUserIsNoop() {
        assertDoesNotThrow(() -> registry.disconnectUser(999L));
    }
}
