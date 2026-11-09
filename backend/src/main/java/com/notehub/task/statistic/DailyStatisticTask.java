package com.notehub.task.statistic;

import com.notehub.service.StatisticService;
import lombok.extern.log4j.Log4j2;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 每日 23:59 生成统计快照
 * 落库用 upsert 保证幂等: 重复执行/补跑都不会产生重复行
 */
@Log4j2
@Component
public class DailyStatisticTask {

    private final StatisticService statisticService;

    public DailyStatisticTask(StatisticService statisticService) {
        this.statisticService = statisticService;
    }

    @Scheduled(cron = "0 59 23 * * ?")
    public void run() {
        try {
            statisticService.snapshotToday();
        } catch (Exception e) {
            // 定时任务异常不能外抛(会中断调度线程), 记录后等下次
            log.error("[统计] 当日快照生成失败: {}", e.getMessage(), e);
        }
    }
}
