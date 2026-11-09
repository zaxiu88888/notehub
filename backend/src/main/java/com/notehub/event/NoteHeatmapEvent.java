package com.notehub.event;

import java.time.LocalDate;

/**
 * 笔记产出变化 → 维护热力图位图; 删除场景在事务提交后消费, 回滚不污染缓存
 */
public record NoteHeatmapEvent(Object source, Long authorId, LocalDate day, int delta) {
}
