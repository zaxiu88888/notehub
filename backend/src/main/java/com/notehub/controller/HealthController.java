package com.notehub.controller;

import com.notehub.common.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 健康检查 / 冒烟入口
 */
@RestController
@RequestMapping("/api")
public class HealthController {

    @GetMapping("/hello")
    public ApiResponse<Map<String, Object>> hello() {
        return ApiResponse.ok(Map.of(
                "message", "notehub ok",
                "time", LocalDateTime.now().toString()
        ));
    }
}
