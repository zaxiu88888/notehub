package com.notehub.task.upload;

import com.notehub.service.ImageCleanupService;
import lombok.extern.log4j.Log4j2;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 每周扫描 uploads/ 删除无引用且超过 24h 的图片(兜底: 直接上传但未保存笔记等场景)
 */
@Log4j2
@Component
public class UploadCleanupTask {

    private final ImageCleanupService imageCleanupService;

    public UploadCleanupTask(ImageCleanupService imageCleanupService) {
        this.imageCleanupService = imageCleanupService;
    }

    @Scheduled(cron = "${upload.cleanup-cron:0 30 3 * * MON}")
    public void cleanupOrphans() {
        try {
            imageCleanupService.sweepOrphans();
        } catch (Exception e) {
            log.error("[上传清理] 定时任务失败: {}", e.getMessage(), e);
        }
    }
}
