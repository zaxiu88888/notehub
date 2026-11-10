package com.notehub.task.upload;

import com.notehub.service.ImageCleanupService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UploadCleanupTaskTest {

    @Test
    @DisplayName("定时任务: 调用全量扫描; 异常不外抛")
    void cleanupOrphansIsSafe() {
        ImageCleanupService service = mock(ImageCleanupService.class);
        UploadCleanupTask task = new UploadCleanupTask(service);

        task.cleanupOrphans();
        verify(service).sweepOrphans();

        when(service.sweepOrphans()).thenThrow(new IllegalStateException("io"));
        task.cleanupOrphans(); // 不抛异常
    }
}
