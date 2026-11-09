package com.notehub.service;

import com.notehub.service.impl.UnreadPushServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class UnreadPushServiceImplTest {

    private SimpMessagingTemplate messagingTemplate;
    private MessageService messageService;
    private UnreadPushServiceImpl service;

    @BeforeEach
    void setUp() {
        messagingTemplate = mock(SimpMessagingTemplate.class);
        messageService = mock(MessageService.class);
        service = new UnreadPushServiceImpl(messagingTemplate, messageService);
    }

    @Test
    @DisplayName("推送: 目标 /user/{id}/queue/unread, 载荷为未读计数")
    void pushesUnreadCounts() {
        Map<String, Long> counts = Map.of("total", 3L, "like", 1L, "comment", 2L, "system", 0L);
        when(messageService.unreadCount(2L)).thenReturn(counts);

        service.pushUnread(2L);

        verify(messagingTemplate).convertAndSendToUser("2", "/queue/unread", counts);
    }

    @Test
    @DisplayName("推送失败/空用户: 静默, 不影响主流程")
    void pushFailureIsSilent() {
        service.pushUnread(null);
        verifyNoInteractions(messagingTemplate);

        when(messageService.unreadCount(3L)).thenThrow(new IllegalStateException("db down"));
        service.pushUnread(3L);
        verifyNoInteractions(messagingTemplate);
    }
}
