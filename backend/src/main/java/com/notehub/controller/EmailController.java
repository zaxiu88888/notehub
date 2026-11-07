package com.notehub.controller;

import com.notehub.common.ApiResponse;
import com.notehub.service.EmailService;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 邮件验证码
 */
@RestController
@RequestMapping("/api/email")
@Validated
public class EmailController {

    private final EmailService emailService;

    public EmailController(EmailService emailService) {
        this.emailService = emailService;
    }

    @GetMapping("/verify-code")
    public ApiResponse<Map<String, Object>> sendVerifyCode(
            @RequestParam @NotBlank @Email String email) {
        String devCode = emailService.sendVerifyCode(email);
        // devCode 仅在邮件开关关闭时非空, 供本地联调; 生产配置下不下发该字段
        if (devCode == null) {
            return ApiResponse.ok(Map.of("sent", true));
        }
        return ApiResponse.ok(Map.of("sent", true, "devCode", devCode));
    }
}
