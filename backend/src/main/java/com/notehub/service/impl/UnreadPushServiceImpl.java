package com.notehub.service.impl;

import com.notehub.service.MessageService;
import com.notehub.service.UnreadPushService;
import lombok.extern.log4j.Log4j2;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 推送 /user/{userId}/queue/unread, 载荷与 GET /api/messages/unread/count 一致
 */
@Log4j2
@Service
public class UnreadPushServiceImpl implements UnreadPushService {

    private final SimpMessagingTemplate messagingTemplate;
    private final MessageService messageService;

    public UnreadPushServiceImpl(SimpMessagingTemplate messagingTemplate,
                                 MessageService messageService) {
        this.messagingTemplate = messagingTemplate;
        this.messageService = messageService;
    }

    @Override
    public void pushUnread(Long userId) {
        if (userId == null) {
            return;
        }
        try {
            Map<String, Long> counts = messageService.unreadCount(userId);
            messagingTemplate.convertAndSendToUser(
                    String.valueOf(userId), "/queue/unread", counts);
        } catch (Exception e) {
            // 推送失败不影响消息落库/已读操作
            log.warn("[WS] 未读数推送失败: user={}, err={}", userId, e.getMessage());
        }
    }
}
