package com.notehub.service;

import com.notehub.TestObjectMapper;
import com.notehub.common.BusinessException;
import com.notehub.common.ErrorCode;
import com.notehub.model.enums.RedisKey;
import com.notehub.service.impl.EmailServiceImpl;
import com.notehub.task.email.EmailTask;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EmailServiceImplTest {

    private RedisService redisService;
    private JavaMailSender mailSender;
    private EmailServiceImpl devModeService;   // enabled=false
    private EmailServiceImpl realModeService;  // enabled=true

    @BeforeEach
    void setUp() {
        redisService = mock(RedisService.class);
        mailSender = mock(JavaMailSender.class);
        devModeService = new EmailServiceImpl(redisService, TestObjectMapper.get(), mailSender,
                false, 300, 60, "test@notehub.dev");
        realModeService = new EmailServiceImpl(redisService, TestObjectMapper.get(), mailSender,
                true, 300, 60, "test@notehub.dev");
    }

    @Test
    @DisplayName("邮箱格式非法 → BAD_PARAM")
    void invalidEmailFormat() {
        BusinessException e = assertThrows(BusinessException.class,
                () -> devModeService.sendVerifyCode("not-an-email"));
        assertEquals(ErrorCode.BAD_PARAM, e.getErrorCode());
    }

    @Test
    @DisplayName("60s 限流: setIfAbsent 失败 → CODE_TOO_FREQUENT")
    void rateLimited() {
        when(redisService.setIfAbsent(contains("limit"), anyString(), any(Duration.class))).thenReturn(false);

        BusinessException e = assertThrows(BusinessException.class,
                () -> devModeService.sendVerifyCode("a@b.com"));
        assertEquals(ErrorCode.CODE_TOO_FREQUENT, e.getErrorCode());
    }

    @Test
    @DisplayName("dev 模式: 不入队不发信, 验证码直接写 Redis 并返回明文")
    void devModeStoresCodeDirectly() {
        when(redisService.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);

        String code = devModeService.sendVerifyCode("a@b.com");

        assertEquals(6, code.length()); // 6 位数字验证码
        verify(redisService).set(eq(RedisKey.verifyCode("a@b.com")), eq(code), any(Duration.class));
        verify(redisService, never()).leftPush(anyString(), anyString());
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("真发模式: 任务 JSON 入队, 不直接写 Redis 不返回明文")
    void realModeEnqueuesTask() throws Exception {
        when(redisService.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);

        String code = realModeService.sendVerifyCode("a@b.com");

        assertNull(code);
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(redisService).leftPush(eq(RedisKey.emailTaskQueue()), captor.capture());
        assertTrue(captor.getValue().contains("a@b.com"));
        verify(redisService, never()).set(anyString(), anyString(), any(Duration.class));
    }

    @Test
    @DisplayName("verifyAndConsumeCode: 匹配即消费(删除); 不匹配/过期返回 false")
    void verifyAndConsume() {
        when(redisService.get(RedisKey.verifyCode("a@b.com"))).thenReturn("654321");

        assertFalse(devModeService.verifyAndConsumeCode("a@b.com", "000000"));
        assertTrue(devModeService.verifyAndConsumeCode("a@b.com", "654321"));
        verify(redisService).delete(RedisKey.verifyCode("a@b.com"));
    }

    @Test
    @DisplayName("deliverTask: 发信并把验证码落 Redis")
    void deliverTaskSendsAndStores() {
        EmailTask task = new EmailTask("a@b.com", "111222", System.currentTimeMillis());

        realModeService.deliverTask(task);

        verify(mailSender).send(any(SimpleMailMessage.class));
        verify(redisService).set(eq(RedisKey.verifyCode("a@b.com")), eq("111222"), any(Duration.class));
    }

    @Test
    @DisplayName("deliverTask 发信失败应向上抛出(由消费者兜底记录)")
    void deliverTaskPropagatesFailure() {
        EmailTask task = new EmailTask("a@b.com", "111222", System.currentTimeMillis());
        doThrow(new RuntimeException("smtp down")).when(mailSender).send(any(SimpleMailMessage.class));

        assertThrows(RuntimeException.class, () -> realModeService.deliverTask(task));
        verify(redisService, never()).set(anyString(), anyString(), any(Duration.class));
    }
}
