package com.notehub.service;

import com.notehub.common.PageVO;
import com.notehub.model.dto.message.MessageQueryParams;
import com.notehub.model.vo.message.MessageVO;

import java.util.Map;

/**
 * 站内消息服务
 */
public interface MessageService {

    PageVO<MessageVO> listMessages(Long userId, MessageQueryParams params);

    /** 分类型未读数: {total, like, comment, system} */
    Map<String, Long> unreadCount(Long userId);

    void markRead(Long userId, Long messageId);

    void markAllRead(Long userId);

    void markBatchRead(Long userId, java.util.List<Long> messageIds);

    void deleteMessage(Long userId, Long messageId);
}
