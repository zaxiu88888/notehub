package com.notehub.security;

import com.notehub.mapper.UserMapper;
import com.notehub.model.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StompAuthChannelInterceptorTest {

    private JwtUtil jwtUtil;
    private UserMapper userMapper;
    private StompAuthChannelInterceptor interceptor;
    private final MessageChannel channel = mock(MessageChannel.class);

    @BeforeEach
    void setUp() {
        jwtUtil = mock(JwtUtil.class);
        userMapper = mock(UserMapper.class);
        interceptor = new StompAuthChannelInterceptor(jwtUtil, userMapper);
    }

    private User user(long id, int isBanned) {
        User user = new User();
        user.setUserId(id);
        user.setIsBanned(isBanned);
        return user;
    }

    private record Frame(Message<byte[]> message, StompHeaderAccessor accessor) {
    }

    private Frame frame(StompCommand command, String authorization, String destination) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(command);
        // 真实 CONNECT/SUBSCRIBE 帧总带有原生头; 补一个让 MessageHeaders 保留 accessor
        accessor.addNativeHeader("accept-version", "1.2");
        if (authorization != null) {
            accessor.addNativeHeader("Authorization", authorization);
        }
        if (destination != null) {
            accessor.setDestination(destination);
        }
        accessor.setLeaveMutable(true);
        return new Frame(MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders()), accessor);
    }

    @Test
    @DisplayName("CONNECT: Bearer JWT 有效 → 绑定 Principal(userId)")
    void connectWithValidTokenBindsPrincipal() {
        when(jwtUtil.parseUserId("good-token")).thenReturn(7L);
        when(userMapper.selectById(7L)).thenReturn(user(7L, 0));
        Frame frame = frame(StompCommand.CONNECT, "Bearer good-token", null);

        Message<?> result = interceptor.preSend(frame.message(), channel);

        assertSame(frame.message(), result);
        assertNotNull(frame.accessor().getUser());
        assertEquals("7", frame.accessor().getUser().getName());
    }

    @Test
    @DisplayName("CONNECT: token 有效但用户已封禁/已删除 → 拒绝")
    void connectBannedOrMissingUserRejected() {
        when(jwtUtil.parseUserId("banned-token")).thenReturn(8L);
        when(userMapper.selectById(8L)).thenReturn(user(8L, 1));
        assertThrows(MessagingException.class,
                () -> interceptor.preSend(frame(StompCommand.CONNECT, "Bearer banned-token", null).message(),
                        channel));

        when(jwtUtil.parseUserId("deleted-token")).thenReturn(9L);
        when(userMapper.selectById(9L)).thenReturn(null);
        assertThrows(MessagingException.class,
                () -> interceptor.preSend(frame(StompCommand.CONNECT, "Bearer deleted-token", null).message(),
                        channel));
    }

    @Test
    @DisplayName("CONNECT: 无 token/无效 token → 拒绝握手")
    void connectWithoutValidTokenRejected() {
        assertThrows(MessagingException.class,
                () -> interceptor.preSend(frame(StompCommand.CONNECT, null, null).message(), channel));

        when(jwtUtil.parseUserId("bad-token")).thenReturn(null);
        assertThrows(MessagingException.class,
                () -> interceptor.preSend(frame(StompCommand.CONNECT, "Bearer bad-token", null).message(),
                        channel));
    }

    @Test
    @DisplayName("SUBSCRIBE: 只允许 /user/queue/**")
    void subscribeOnlyUserQueue() {
        Message<?> allowed = frame(StompCommand.SUBSCRIBE, null, "/user/queue/unread").message();
        assertSame(allowed, interceptor.preSend(allowed, channel));

        assertThrows(MessagingException.class,
                () -> interceptor.preSend(
                        frame(StompCommand.SUBSCRIBE, null, "/queue/unread-2").message(), channel));
    }

    @Test
    @DisplayName("SEND: 不支持客户端上行消息")
    void sendRejected() {
        assertThrows(MessagingException.class,
                () -> interceptor.preSend(frame(StompCommand.SEND, null, "/queue/x").message(), channel));
    }

    @Test
    @DisplayName("非 STOMP 消息: 原样放行")
    void nonStompMessagePassesThrough() {
        Message<byte[]> message = MessageBuilder.withPayload(new byte[0]).build();
        assertSame(message, interceptor.preSend(message, channel));
    }
}
