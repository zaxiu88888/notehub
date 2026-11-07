package com.notehub.task.email;

import com.notehub.TestObjectMapper;
import com.notehub.service.EmailService;
import com.notehub.service.RedisService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EmailTaskConsumerTest {

    private RedisService redisService;
    private EmailService emailService;
    private EmailTaskConsumer consumer;

    @BeforeEach
    void setUp() {
        redisService = mock(RedisService.class);
        emailService = mock(EmailService.class);
        consumer = new EmailTaskConsumer(redisService, emailService, TestObjectMapper.get());
    }

    @Test
    @DisplayName("队列非空: 按右出顺序逐个投递, 队列空则停止")
    void consumesUntilEmpty() {
        when(redisService.rightPop("queue:email:task")).thenReturn(
                "{\"email\":\"a@b.com\",\"code\":\"111111\",\"timestamp\":1}",
                "{\"email\":\"c@d.com\",\"code\":\"222222\",\"timestamp\":2}",
                null);

        consumer.consume();

        InOrder inOrder = inOrder(emailService);
        inOrder.verify(emailService).deliverTask(Mockito.argThat(t -> "a@b.com".equals(t.getEmail())));
        inOrder.verify(emailService).deliverTask(Mockito.argThat(t -> "c@d.com".equals(t.getEmail())));
        verify(emailService, Mockito.times(2)).deliverTask(any()); // 恰好两次
    }

    @Test
    @DisplayName("空队列: 静默返回")
    void consumesEmptyQueue() {
        when(redisService.rightPop("queue:email:task")).thenReturn(null);
        consumer.consume();
        verify(emailService, never()).deliverTask(any());
    }

    @Test
    @DisplayName("坏 JSON: 记日志丢弃, 不中断循环")
    void skipsMalformedJson() {
        when(redisService.rightPop("queue:email:task")).thenReturn("not-a-json", null);

        assertDoesNotThrow(() -> consumer.consume());
        verify(emailService, never()).deliverTask(any());
    }

    @Test
    @DisplayName("单封发送失败: 不中断后续任务")
    void continuesAfterSendFailure() {
        when(redisService.rightPop("queue:email:task")).thenReturn(
                "{\"email\":\"bad@b.com\",\"code\":\"111111\",\"timestamp\":1}",
                "{\"email\":\"good@d.com\",\"code\":\"222222\",\"timestamp\":2}",
                null);
        doThrow(new RuntimeException("smtp down")).when(emailService)
                .deliverTask(Mockito.argThat(t -> "bad@b.com".equals(t.getEmail())));

        assertDoesNotThrow(() -> consumer.consume());
        verify(emailService).deliverTask(Mockito.argThat(t -> "good@d.com".equals(t.getEmail())));
    }
}
