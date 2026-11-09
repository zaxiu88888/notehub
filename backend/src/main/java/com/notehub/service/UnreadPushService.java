package com.notehub.service;

/**
 * I3: 未读消息 WebSocket 推送
 */
public interface UnreadPushService {

    /** 向在线用户推送最新未读计数; 用户离线/推送失败静默 */
    void pushUnread(Long userId);
}
