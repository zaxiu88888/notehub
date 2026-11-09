package com.notehub.event;

import com.notehub.mapper.MessageMapper;
import com.notehub.model.entity.Message;
import com.notehub.service.UnreadPushService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InteractionEventListenerTest {

    @Test
    @DisplayName("互动消息落库后推送未读数")
    void pushesAfterInsert() {
        MessageMapper messageMapper = mock(MessageMapper.class);
        UnreadPushService pushService = mock(UnreadPushService.class);
        InteractionEventListener listener = new InteractionEventListener(messageMapper, pushService);

        listener.onInteraction(InteractionEvent.likeNote(this, 2L, 3L, 9L));

        verify(messageMapper).insert(any(Message.class));
        verify(pushService).pushUnread(2L);
    }

    @Test
    @DisplayName("落库失败: 不推送, 异常被吞掉")
    void noPushWhenInsertFails() {
        MessageMapper messageMapper = mock(MessageMapper.class);
        UnreadPushService pushService = mock(UnreadPushService.class);
        when(messageMapper.insert(any(Message.class))).thenThrow(new IllegalStateException("db down"));
        InteractionEventListener listener = new InteractionEventListener(messageMapper, pushService);

        listener.onInteraction(InteractionEvent.likeNote(this, 2L, 3L, 9L));

        verify(pushService, never()).pushUnread(anyLong());
    }
}
