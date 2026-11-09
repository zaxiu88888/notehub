package com.notehub.service;

import com.notehub.model.entity.Statistic;
import com.notehub.model.vo.statistic.StatisticVO;

/**
 * 统计服务
 */
public interface StatisticService {

    /** 管理看板: 今日实时指标 + 最近趋势快照 */
    StatisticVO getDashboard(int trendDays);

    /** 生成当日快照并落库(幂等 upsert, 定时任务调用) */
    Statistic snapshotToday();
}
