package com.notehub.config;

import com.notehub.security.StompAuthChannelInterceptor;
import com.notehub.security.WsSessionRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;
import org.springframework.web.socket.handler.WebSocketHandlerDecorator;

import java.util.Arrays;

/**
 * STOMP WebSocket: 只用于服务端 → 客户端推送未读数
 * - 端点 /ws, 不使用 SockJS(前端原生 WebSocket)
 * - 简单代理仅 /queue; 用户目标前缀 /user, 客户端订阅 /user/queue/unread
 * - CONNECT 帧携带 JWT, 由 StompAuthChannelInterceptor 校验并绑定 Principal
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final StompAuthChannelInterceptor authChannelInterceptor;
    private final WsSessionRegistry wsSessionRegistry;
    private final String[] allowedOrigins;

    public WebSocketConfig(StompAuthChannelInterceptor authChannelInterceptor,
                           WsSessionRegistry wsSessionRegistry,
                           @Value("${app.ws.allowed-origins:http://localhost:5173}") String allowedOrigins) {
        this.authChannelInterceptor = authChannelInterceptor;
        this.wsSessionRegistry = wsSessionRegistry;
        this.allowedOrigins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .toArray(String[]::new);
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws").setAllowedOriginPatterns(allowedOrigins);
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/queue");
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(authChannelInterceptor);
    }

    /** 记录底层 WebSocketSession, 供封禁时主动关闭; 连接关闭时清理注册表 */
    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration registration) {
        registration.addDecoratorFactory(handler -> new WebSocketHandlerDecorator(handler) {
            @Override
            public void afterConnectionEstablished(WebSocketSession session) throws Exception {
                wsSessionRegistry.registerSession(session);
                super.afterConnectionEstablished(session);
            }

            @Override
            public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus)
                    throws Exception {
                wsSessionRegistry.unregisterSession(session.getId());
                super.afterConnectionClosed(session, closeStatus);
            }
        });
    }
}
