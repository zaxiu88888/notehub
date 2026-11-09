package com.notehub.controller;

import com.notehub.common.ApiResponse;
import com.notehub.model.vo.statistic.StatisticVO;
import com.notehub.security.NeedAdmin;
import com.notehub.service.StatisticService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理看板 /api/statistic
 */
@RestController
@RequestMapping("/api")
public class StatisticController {

    private final StatisticService statisticService;

    public StatisticController(StatisticService statisticService) {
        this.statisticService = statisticService;
    }

    @GetMapping("/statistic")
    @NeedAdmin
    public ApiResponse<StatisticVO> dashboard(@RequestParam(defaultValue = "30") int days) {
        return ApiResponse.ok(statisticService.getDashboard(days));
    }
}
