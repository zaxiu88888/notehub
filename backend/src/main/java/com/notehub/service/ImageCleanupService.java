package com.notehub.service;

/**
 * I5 上传旧文件清理: 只删除"本地 /images/ 且无任何引用"的图片
 */
public interface ImageCleanupService {

    /** 笔记删除后清理正文图片 */
    void cleanupNoteImages(String content);

    /** 笔记编辑后清理被移除的图片 */
    void cleanupRemovedImages(String oldContent, String newContent);

    /** 换头像后清理旧头像 */
    void cleanupAvatar(String oldAvatarUrl);

    /** 定时全量扫描孤儿文件(跳过 24h 内新文件), 返回删除数 */
    int sweepOrphans();
}
