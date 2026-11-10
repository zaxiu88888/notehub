package com.notehub.service.impl;

import com.notehub.mapper.UserMapper;
import com.notehub.model.entity.User;
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
    private final UserMapper userMapper;

    public UnreadPushServiceImpl(SimpMessagingTemplate messagingTemplate,
                                 MessageService messageService,
                                 UserMapper userMapper) {
        this.messagingTemplate = messagingTemplate;
        this.messageService = messageService;
        this.userMapper = userMapper;
    }

    @Override
    public void pushUnread(Long userId) {
        if (userId == null) {
            return;
        }
        try {
            // 封禁/删除用户不再推送: 覆盖"连接已建立但尚未被断开"的窗口
            User user = userMapper.selectById(userId);
            if (user == null || (user.getIsBanned() != null && user.getIsBanned() == 1)) {
                log.debug("[WS] 跳过封禁/不存在用户推送: userId={}", userId);
                return;
            }
            Map<String, Long> counts = messageService.unreadCount(userId);
            messagingTemplate.convertAndSendToUser(
                    String.valueOf(userId), "/queue/unread", counts);
        } catch (Exception e) {
            // 推送失败不影响消息落库/已读操作
            log.warn("[WS] 未读数推送失败: user={}, err={}", userId, e.getMessage());
        }
    }
}
