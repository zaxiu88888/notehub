package com.notehub.event;

import com.notehub.service.NoteAnalyticsService;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 笔记增删 → 热力图位图增量
 * AFTER_COMMIT: 删除回滚不会污染缓存; fallbackExecution: 发布方无事务时立即执行
 */
@Log4j2
@Component
public class NoteHeatmapListener {

    private final NoteAnalyticsService noteAnalyticsService;

    public NoteHeatmapListener(NoteAnalyticsService noteAnalyticsService) {
        this.noteAnalyticsService = noteAnalyticsService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onNoteHeatmap(NoteHeatmapEvent event) {
        log.debug("[热力图] 事件: user={}, day={}, delta={}",
                event.authorId(), event.day(), event.delta());
        noteAnalyticsService.recordHeatmap(event.authorId(), event.day(), event.delta());
    }
}
