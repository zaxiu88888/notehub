package com.notehub.task.email;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.notehub.model.enums.RedisKey;
import com.notehub.service.EmailService;
import com.notehub.service.RedisService;
import lombok.extern.log4j.Log4j2;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 邮件队列消费者: 每 3s 轮询 Redis List, 逐个真发验证码邮件
 * 单实例消费 MVP 规模足够; 发信失败不回队(验证码场景重发即可), 记 error 日志
 */
@Log4j2
@Component
public class EmailTaskConsumer {

    private final RedisService redisService;
    private final EmailService emailService;
    private final ObjectMapper objectMapper;

    public EmailTaskConsumer(RedisService redisService, EmailService emailService,
                             ObjectMapper objectMapper) {
        this.redisService = redisService;
        this.emailService = emailService;
        this.objectMapper = objectMapper;
    }

    @Scheduled(fixedDelay = 3000)
    public void consume() {
        String queueKey = RedisKey.emailTaskQueue();
        while (true) {
            String json = redisService.rightPop(queueKey);
            if (json == null) {
                break;
            }
            try {
                EmailTask task = objectMapper.readValue(json, EmailTask.class);
                emailService.deliverTask(task);
            } catch (JsonProcessingException e) {
                log.error("[邮件队列] 任务反序列化失败, 丢弃: {}", json, e);
            } catch (Exception e) {
                log.error("[邮件队列] 发送失败, 任务丢弃(用户可重新获取验证码): {}", json, e);
            }
        }
    }
}
