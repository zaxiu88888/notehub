package com.notehub.task.statistic;

import com.notehub.service.StatisticService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class DailyStatisticTaskTest {

    @Test
    @DisplayName("正常执行: 调用快照服务一次")
    void runsSnapshot() {
        StatisticService service = mock(StatisticService.class);
        new DailyStatisticTask(service).run();
        verify(service).snapshotToday();
    }

    @Test
    @DisplayName("快照异常: 被吞掉不外抛(避免打断调度线程)")
    void swallowsException() {
        StatisticService service = mock(StatisticService.class);
        doThrow(new RuntimeException("db down")).when(service).snapshotToday();

        assertDoesNotThrow(() -> new DailyStatisticTask(service).run());
    }
}
