package com.notehub.controller;

import com.notehub.common.ApiResponse;
import com.notehub.common.RateLimit;
import com.notehub.common.RequestContext;
import com.notehub.model.vo.user.CheckinStatusVO;
import com.notehub.security.NeedLogin;
import com.notehub.service.CheckinService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 每日打卡 /api/checkin
 */
@RestController
@RequestMapping("/api/checkin")
public class CheckinController {

    private final CheckinService checkinService;
    private final RequestContext requestContext;

    public CheckinController(CheckinService checkinService, RequestContext requestContext) {
        this.checkinService = checkinService;
        this.requestContext = requestContext;
    }

    @PostMapping
    @NeedLogin
    @RateLimit(key = "checkin", limit = 5)
    public ApiResponse<CheckinStatusVO> checkin() {
        return ApiResponse.ok("打卡成功", checkinService.checkin(requestContext.requireUserId()));
    }

    @GetMapping("/status")
    @NeedLogin
    public ApiResponse<CheckinStatusVO> status() {
        return ApiResponse.ok(checkinService.status(requestContext.requireUserId()));
    }
}
