package com.notehub.service.impl;

import cn.hutool.core.util.RandomUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.notehub.common.BusinessException;
import com.notehub.common.ErrorCode;
import com.notehub.model.enums.RedisKey;
import com.notehub.service.EmailService;
import com.notehub.service.RedisService;
import com.notehub.task.email.EmailTask;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.regex.Pattern;

/**
 * 邮件验证码:
 * - enabled=true: 任务 JSON 入 Redis 队列, EmailTaskConsumer 3s 轮询真发 → 发信后验证码入 Redis(5min)
 * - enabled=false(dev 开关): 直接写 Redis 并把验证码返回给前端联调, 不发邮件
 * 发送频率: setIfAbsent 限流键 TTL limit-seconds
 */
@Log4j2
@Service
public class EmailServiceImpl implements EmailService {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+$");

    private final RedisService redisService;
    private final ObjectMapper objectMapper;
    private final JavaMailSender mailSender; // dev 开关关闭时可能无真实 SMTP, 延迟到发信才使用
    private final boolean mailEnabled;
    private final long expireSeconds;
    private final long limitSeconds;
    private final String from;

    public EmailServiceImpl(RedisService redisService,
                            ObjectMapper objectMapper,
                            JavaMailSender mailSender,
                            @Value("${mail.verify-code.enabled:false}") boolean mailEnabled,
                            @Value("${mail.verify-code.expire-seconds:300}") long expireSeconds,
                            @Value("${mail.verify-code.limit-seconds:60}") long limitSeconds,
                            @Value("${spring.mail.username:notehub@local.dev}") String from) {
        this.redisService = redisService;
        this.objectMapper = objectMapper;
        this.mailSender = mailSender;
        this.mailEnabled = mailEnabled;
        this.expireSeconds = expireSeconds;
        this.limitSeconds = limitSeconds;
        this.from = from;
    }

    @Override
    public String sendVerifyCode(String email) {
        if (email == null || !EMAIL_PATTERN.matcher(email).matches()) {
            throw BusinessException.of(ErrorCode.BAD_PARAM, "email 格式不正确");
        }
        boolean acquired = redisService.setIfAbsent(
                RedisKey.verifyCodeLimit(email), "1", Duration.ofSeconds(limitSeconds));
        if (!acquired) {
            throw BusinessException.of(ErrorCode.CODE_TOO_FREQUENT);
        }

        String code = RandomUtil.randomNumbers(6);
        if (mailEnabled) {
            try {
                EmailTask task = new EmailTask(email, code, System.currentTimeMillis());
                redisService.leftPush(RedisKey.emailTaskQueue(), objectMapper.writeValueAsString(task));
                log.info("[邮件] 验证码任务已入队: {}", email);
            } catch (JsonProcessingException e) {
                log.error("[邮件] 任务序列化失败: {}", e.getMessage(), e);
                throw BusinessException.of(ErrorCode.INTERNAL_ERROR, "验证码发送失败, 请稍后重试");
            }
            return null;
        }

        // dev 模式: 跳过队列与 SMTP, 直接落 Redis 供注册校验
        redisService.set(RedisKey.verifyCode(email), code, Duration.ofSeconds(expireSeconds));
        log.info("[邮件][dev] 未启用真实发信, 验证码已写入 Redis: {} -> {}", email, code);
        return code;
    }

    @Override
    public boolean verifyAndConsumeCode(String email, String code) {
        String key = RedisKey.verifyCode(email);
        String cached = redisService.get(key);
        if (cached == null || !cached.equals(code)) {
            return false;
        }
        redisService.delete(key); // 一次性
        return true;
    }

    @Override
    public void deliverTask(EmailTask task) {        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(task.getEmail());
        message.setSubject("notehub 注册验证码");
        message.setText("您的验证码是: " + task.getCode() + ", " + expireSeconds / 60 + " 分钟内有效, 请勿泄露。");
        mailSender.send(message);
        redisService.set(RedisKey.verifyCode(task.getEmail()), task.getCode(), Duration.ofSeconds(expireSeconds));
        log.info("[邮件] 验证码已发送: {}", task.getEmail());
    }
}
