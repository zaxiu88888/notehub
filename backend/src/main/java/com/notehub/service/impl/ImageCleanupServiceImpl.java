package com.notehub.service.impl;

import com.notehub.mapper.NoteMapper;
import com.notehub.mapper.UserMapper;
import com.notehub.service.ImageCleanupService;
import com.notehub.service.UploadService;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Log4j2
@Service
public class ImageCleanupServiceImpl implements ImageCleanupService {

    private static final Pattern MD_IMAGE = Pattern.compile(
            "!\\[[^\\]]*]\\(\\s*(/images/[^)\\s]+)");
    private static final Pattern HTML_IMAGE = Pattern.compile(
            "<img[^>]+src=[\"'](/images/[^\"']+)", Pattern.CASE_INSENSITIVE);
    private static final Duration SWEEP_MIN_AGE = Duration.ofHours(24);

    private final UploadService uploadService;
    private final NoteMapper noteMapper;
    private final UserMapper userMapper;

    public ImageCleanupServiceImpl(UploadService uploadService, NoteMapper noteMapper,
                                   UserMapper userMapper) {
        this.uploadService = uploadService;
        this.noteMapper = noteMapper;
        this.userMapper = userMapper;
    }

    @Override
    public void cleanupNoteImages(String content) {
        Set<String> urls = extractImageUrls(content);
        if (urls.isEmpty()) {
            return;
        }
        int deleted = 0;
        for (String url : urls) {
            if (!isReferenced(url) && uploadService.deleteByUrl(url)) {
                deleted++;
            }
        }
        log.info("[上传清理] 笔记图片: candidates={}, deleted={}", urls.size(), deleted);
    }

    @Override
    public void cleanupRemovedImages(String oldContent, String newContent) {
        Set<String> removed = extractImageUrls(oldContent);
        removed.removeAll(extractImageUrls(newContent));
        for (String url : removed) {
            if (!isReferenced(url)) {
                uploadService.deleteByUrl(url);
            }
        }
    }

    @Override
    public void cleanupAvatar(String oldAvatarUrl) {
        if (oldAvatarUrl != null && !isReferenced(oldAvatarUrl)) {
            uploadService.deleteByUrl(oldAvatarUrl);
        }
    }

    @Override
    public int sweepOrphans() {
        int deleted = 0;
        for (String url : uploadService.listStoredUrls(SWEEP_MIN_AGE)) {
            if (!isReferenced(url) && uploadService.deleteByUrl(url)) {
                deleted++;
            }
        }
        log.info("[上传清理] 全量扫描完成: deleted={}", deleted);
        return deleted;
    }

    private boolean isReferenced(String url) {
        return noteMapper.countByContentContaining(url) > 0
                || userMapper.countByAvatarUrl(url) > 0;
    }

    /** 提取 Markdown/HTML 图片中的本地 URL, 去重 */
    Set<String> extractImageUrls(String content) {
        Set<String> urls = new LinkedHashSet<>();
        if (content == null || content.isEmpty()) {
            return urls;
        }
        collect(MD_IMAGE, content, urls);
        collect(HTML_IMAGE, content, urls);
        return urls;
    }

    private void collect(Pattern pattern, String content, Set<String> urls) {
        Matcher matcher = pattern.matcher(content);
        while (matcher.find()) {
            urls.add(matcher.group(1));
        }
    }
}
