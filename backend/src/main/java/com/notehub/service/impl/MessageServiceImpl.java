package com.notehub.service.impl;

import com.notehub.common.PageVO;
import com.notehub.mapper.MessageMapper;
import com.notehub.model.dto.message.MessageQueryParams;
import com.notehub.model.entity.Message;
import com.notehub.model.enums.MessageType;
import com.notehub.model.vo.message.MessageVO;
import com.notehub.service.MessageService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class MessageServiceImpl implements MessageService {

    private final MessageMapper messageMapper;

    public MessageServiceImpl(MessageMapper messageMapper) {
        this.messageMapper = messageMapper;
    }

    @Override
    public PageVO<MessageVO> listMessages(Long userId, MessageQueryParams params) {
        int pageSize = Math.min(Math.max(params.getPageSize(), 1), 50);
        int page = Math.max(params.getPage(), 1);
        params.setReceiverId(userId);
        params.setPage(page);
        params.setPageSize(pageSize);

        long total = messageMapper.countByQuery(params);
        List<MessageVO> list = messageMapper.listByQuery(params, (page - 1) * pageSize, pageSize)
                .stream().map(this::toVO).toList();
        return PageVO.of(list, page, pageSize, total);
    }

    @Override
    public Map<String, Long> unreadCount(Long userId) {
        Map<Integer, Long> byType = new HashMap<>();
        for (Map<String, Object> row : messageMapper.countUnreadGroupByType(userId)) {
            byType.put(((Number) row.get("type")).intValue(), ((Number) row.get("cnt")).longValue());
        }
        Map<String, Long> result = new HashMap<>();
        result.put("total", messageMapper.countUnread(userId));
        result.put("like", byType.getOrDefault(MessageType.LIKE, 0L));
        result.put("comment", byType.getOrDefault(MessageType.COMMENT, 0L));
        result.put("system", byType.getOrDefault(MessageType.SYSTEM, 0L));
        return result;
    }

    @Override
    public void markRead(Long userId, Long messageId) {
        messageMapper.markRead(messageId, userId); // 未读才置读, 幂等
    }

    @Override
    @Transactional
    public void markAllRead(Long userId) {
        messageMapper.markAllRead(userId);
    }

    @Override
    public void markBatchRead(Long userId, List<Long> messageIds) {
        if (messageIds == null || messageIds.isEmpty()) {
            return;
        }
        messageMapper.markBatchRead(userId, messageIds);
    }

    @Override
    public void deleteMessage(Long userId, Long messageId) {
        messageMapper.delete(messageId, userId);
    }

    private MessageVO toVO(Message message) {
        MessageVO vo = new MessageVO();
        vo.setMessageId(message.getMessageId());
        vo.setType(message.getType());
        vo.setTargetType(message.getTargetType());
        vo.setTargetId(message.getTargetId());
        vo.setContent(message.getContent());
        vo.setIsRead(message.getIsRead());
        vo.setCreatedAt(message.getCreatedAt());
        if (message.getSenderId() != null && message.getSenderId() > 0) {
            MessageVO.SenderVO sender = new MessageVO.SenderVO();
            sender.setUserId(message.getSenderId());
            sender.setUsername(message.getSenderName());
            sender.setAvatarUrl(message.getSenderAvatarUrl());
            vo.setSender(sender);
        }
        return vo;
    }
}
