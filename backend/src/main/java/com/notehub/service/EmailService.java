package com.notehub.service;

import com.notehub.task.email.EmailTask;

/**
 * 邮件验证码服务
 */
public interface EmailService {

    /**
     * 发送注册验证码
     *
     * @return dev 模式(邮件开关关闭)时返回验证码明文供联调; 真发模式返回 null
     * @throws com.notehub.common.BusinessException CODE_TOO_FREQUENT 60s 内重复发送
     */
    String sendVerifyCode(String email);

    /** 校验并消费验证码(一次性): 匹配返回 true 并删除; 失败/过期返回 false */
    boolean verifyAndConsumeCode(String email, String code);

    /** 队列消费者回调: 真发邮件并把验证码落 Redis (仅 EmailTaskConsumer 调用) */
    void deliverTask(EmailTask task);
}
