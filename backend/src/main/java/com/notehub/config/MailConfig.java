package com.notehub.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

/**
 * 邮件兜底配置:
 * spring.mail.host 未配置时(邮件 dev 开关模式), Boot 的 MailSenderAutoConfiguration 不生效,
 * 但 EmailServiceImpl 构造仍需要 JavaMailSender 实例 —— 此处提供空实现兜底(永远不会被调用,
 * 因为 mail.verify-code.enabled=false 阻断了发信路径); 一旦 local.yaml 配了 spring.mail.*,
 * Boot 自动装配的真实 JavaMailSender 存在, 本 Bean 自动退位。
 */
@Configuration
public class MailConfig {

    @Bean
    @ConditionalOnMissingBean(JavaMailSender.class)
    public JavaMailSender javaMailSender() {
        return new JavaMailSenderImpl();
    }
}
