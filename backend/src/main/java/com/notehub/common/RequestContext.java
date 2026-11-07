package com.notehub.common;

import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

/**
 * 请求级上下文: TokenInterceptor 写入, Service/Aspect 读取
 * 当前登录状态贯穿整个请求生命周期
 * (类名避开 Spring 注解 RequestScope, 用 RequestContext)
 */
@Component
@RequestScope
public class RequestContext {

    private boolean login;
    private Long userId;
    private boolean admin;

    public boolean isLogin() {
        return login && userId != null;
    }

    public void markLogin(Long userId, boolean admin) {
        this.login = true;
        this.userId = userId;
        this.admin = admin;
    }

    public void markAnonymous() {
        this.login = false;
        this.userId = null;
        this.admin = false;
    }

    /**
     * 需要登录态时取当前用户 ID; 未登录抛业务异常(而非返回 null 让调用方判空)
     */
    public Long requireUserId() {
        if (!isLogin()) {
            throw BusinessException.of(ErrorCode.UNAUTHORIZED);
        }
        return userId;
    }

    public Long getUserId() {
        return userId;
    }

    public boolean isAdmin() {
        return admin;
    }
}
