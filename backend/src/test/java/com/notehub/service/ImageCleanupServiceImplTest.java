package com.notehub.service;

import com.notehub.mapper.NoteMapper;
import com.notehub.mapper.UserMapper;
import com.notehub.service.impl.ImageCleanupServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ImageCleanupServiceImplTest {

    private UploadService uploadService;
    private NoteMapper noteMapper;
    private UserMapper userMapper;
    private ImageCleanupServiceImpl service;

    @BeforeEach
    void setUp() {
        uploadService = mock(UploadService.class);
        noteMapper = mock(NoteMapper.class);
        userMapper = mock(UserMapper.class);
        service = new ImageCleanupServiceImpl(uploadService, noteMapper, userMapper);
        when(uploadService.deleteByUrl(anyString())).thenReturn(true);
    }

    @Test
    @DisplayName("笔记删除: 无引用的本地图片删除, 仍被引用的保留")
    void cleanupNoteImagesRespectsReferences() {
        String content = "![a](/images/a.png)\n<img src=\"/images/b.png\">";
        when(noteMapper.countByContentContaining("/images/a.png")).thenReturn(0L);
        when(userMapper.countByAvatarUrl("/images/a.png")).thenReturn(0L);
        when(noteMapper.countByContentContaining("/images/b.png")).thenReturn(1L);

        service.cleanupNoteImages(content);

        verify(uploadService).deleteByUrl("/images/a.png");
        verify(uploadService, never()).deleteByUrl("/images/b.png");
    }

    @Test
    @DisplayName("编辑: 只清理被移除的图片")
    void cleanupRemovedImagesOnlyRemoved() {
        String oldContent = "![a](/images/a.png) ![b](/images/b.png)";
        String newContent = "![b](/images/b.png)";

        service.cleanupRemovedImages(oldContent, newContent);

        verify(uploadService).deleteByUrl("/images/a.png");
        verify(uploadService, never()).deleteByUrl("/images/b.png");
    }

    @Test
    @DisplayName("头像: 旧头像无引用才删; null 跳过")
    void cleanupAvatar() {
        service.cleanupAvatar(null);
        verify(uploadService, never()).deleteByUrl(any());

        service.cleanupAvatar("/images/old.png");
        verify(uploadService).deleteByUrl("/images/old.png");

        when(userMapper.countByAvatarUrl("/images/shared.png")).thenReturn(1L);
        service.cleanupAvatar("/images/shared.png");
        verify(uploadService, never()).deleteByUrl("/images/shared.png");
    }

    @Test
    @DisplayName("全量扫描: 一次构建引用集合, 删除无引用文件")
    void sweepOrphansCountsDeleted() {
        when(uploadService.listStoredUrls(any(Duration.class)))
                .thenReturn(List.of("/images/used.png", "/images/avatar.png", "/images/orphan.png"));
        when(noteMapper.selectAllContents())
                .thenReturn(List.of("![x](/images/used.png)"));
        when(userMapper.selectAllAvatarUrls())
                .thenReturn(List.of("/images/avatar.png"));

        int deleted = service.sweepOrphans();

        assertEquals(1, deleted);
        verify(uploadService).deleteByUrl("/images/orphan.png");
        verify(uploadService, never()).deleteByUrl("/images/used.png");
        verify(uploadService, never()).deleteByUrl("/images/avatar.png");
    }
}
