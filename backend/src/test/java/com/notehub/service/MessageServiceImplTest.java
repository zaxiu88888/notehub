package com.notehub.service;

import com.notehub.common.PageVO;
import com.notehub.mapper.MessageMapper;
import com.notehub.model.dto.message.MessageQueryParams;
import com.notehub.model.entity.Message;
import com.notehub.model.vo.message.MessageVO;
import com.notehub.service.impl.MessageServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MessageServiceImplTest {

    private MessageMapper messageMapper;
    private MessageServiceImpl service;

    @BeforeEach
    void setUp() {
        messageMapper = mock(MessageMapper.class);
        service = new MessageServiceImpl(messageMapper);
    }

    private Message message(long id, int type, long senderId) {
        Message m = new Message();
        m.setMessageId(id);
        m.setReceiverId(7L);
        m.setSenderId(senderId);
        m.setType(type);
        m.setTargetType(1);
        m.setTargetId(10L);
        m.setContent("赞了你的笔记");
        m.setIsRead(0);
        m.setSenderName("触发者" + senderId);
        return m;
    }

    @Test
    @DisplayName("未读数: 按 type 分组, total 独立统计")
    void unreadCountByType() {
        when(messageMapper.countUnreadGroupByType(7L)).thenReturn(List.of(
                Map.of("type", 1, "cnt", 3L),
                Map.of("type", 2, "cnt", 2L)));
        when(messageMapper.countUnread(7L)).thenReturn(5L);

        Map<String, Long> counts = service.unreadCount(7L);

        assertEquals(5L, counts.get("total"));
        assertEquals(3L, counts.get("like"));
        assertEquals(2L, counts.get("comment"));
        assertEquals(0L, counts.get("system"));
    }

    @Test
    @DisplayName("消息列表: 参数回填 receiverId, 系统消息(sender=0)不带 sender 摘要")
    void listMessages() {
        when(messageMapper.countByQuery(any())).thenReturn(2L);
        when(messageMapper.listByQuery(any(), org.mockito.ArgumentMatchers.anyInt(),
                org.mockito.ArgumentMatchers.anyInt()))
                .thenReturn(List.of(message(1L, 1, 9L), message(2L, 3, 0L)));

        MessageQueryParams params = new MessageQueryParams();
        PageVO<MessageVO> page = service.listMessages(7L, params);

        assertEquals(7L, params.getReceiverId()); // 回填校验
        assertEquals(2, page.getList().size());
        assertEquals("触发者9", page.getList().get(0).getSender().getUsername());
        assertEquals(null, page.getList().get(1).getSender()); // 系统消息无 sender
    }

    @Test
    @DisplayName("标记已读/删除: 均以 receiverId 归属过滤")
    void readAndDeleteScope() {
        service.markRead(7L, 100L);
        verify(messageMapper).markRead(100L, 7L);

        service.deleteMessage(7L, 100L);
        verify(messageMapper).delete(100L, 7L);

        service.markBatchRead(7L, List.of(1L, 2L));
        verify(messageMapper).markBatchRead(eq(7L), eq(List.of(1L, 2L)));

        service.markBatchRead(7L, List.of()); // 空列表静默
    }
}
