package com.notehub.service.impl;

import com.notehub.common.BusinessException;
import com.notehub.common.ErrorCode;
import com.notehub.service.UploadService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * 本地磁盘存储: {upload.path}/{uuid}.{ext}, 经 WebConfig 的 /images/** 静态映射访问
 */
@Log4j2
@Service
public class UploadServiceImpl implements UploadService {

    /** 后缀白名单(不收 svg: 可内嵌脚本, 静态域直开有 XSS 风险) */
    private static final Set<String> ALLOWED_EXT = Set.of("jpg", "jpeg", "png", "gif", "webp");
    private static final long MAX_BYTES = 5L * 1024 * 1024;

    private final Path uploadDir;
    private final String urlPrefix;

    public UploadServiceImpl(@Value("${upload.path:./uploads}") String uploadPath,
                             @Value("${upload.url-prefix:/images}") String urlPrefix) {
        this.uploadDir = Paths.get(uploadPath).toAbsolutePath().normalize();
        this.urlPrefix = urlPrefix.endsWith("/") ? urlPrefix.substring(0, urlPrefix.length() - 1) : urlPrefix;
    }

    @Override
    public String saveImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw BusinessException.of(ErrorCode.BAD_PARAM, "文件为空");
        }
        if (file.getSize() > MAX_BYTES) {
            throw BusinessException.of(ErrorCode.BAD_PARAM, "图片不能超过 5MB");
        }
        String ext = extensionOf(file.getOriginalFilename());
        if (!ALLOWED_EXT.contains(ext)) {
            throw BusinessException.of(ErrorCode.BAD_PARAM, "仅支持 jpg/jpeg/png/gif/webp 图片");
        }
        String filename = UUID.randomUUID().toString().replace("-", "") + "." + ext;
        try {
            Files.createDirectories(uploadDir);
            // 文件名已 UUID 化, resolve 不会越出 uploadDir
            file.transferTo(uploadDir.resolve(filename));
        } catch (IOException e) {
            log.error("[上传] 写入失败: {}", e.getMessage(), e);
            throw BusinessException.of(ErrorCode.INTERNAL_ERROR, "图片保存失败");
        }
        return urlPrefix + "/" + filename;
    }

    @Override
    public boolean deleteByUrl(String url) {
        Path file = resolveLocalFile(url);
        if (file == null) {
            return false;
        }
        try {
            boolean deleted = Files.deleteIfExists(file);
            if (deleted) {
                log.info("[上传清理] 已删除: {}", url);
            }
            return deleted;
        } catch (IOException e) {
            log.warn("[上传清理] 删除失败: url={}, err={}", url, e.getMessage());
            return false;
        }
    }

    @Override
    public List<String> listStoredUrls(Duration minAge) {
        if (!Files.isDirectory(uploadDir)) {
            return List.of();
        }
        long cutoff = System.currentTimeMillis() - minAge.toMillis();
        try (Stream<Path> files = Files.list(uploadDir)) {
            return files.filter(Files::isRegularFile)
                    .filter(file -> {
                        try {
                            return Files.getLastModifiedTime(file).toMillis() <= cutoff;
                        } catch (IOException e) {
                            return false;
                        }
                    })
                    .map(file -> urlPrefix + "/" + file.getFileName())
                    .sorted()
                    .toList();
        } catch (IOException | UncheckedIOException e) {
            log.warn("[上传清理] 扫描目录失败: {}", e.getMessage());
            return List.of();
        }
    }

    /** URL → 本地文件; 只接受单层文件名, 防路径穿越 */
    private Path resolveLocalFile(String url) {
        String prefix = urlPrefix + "/";
        if (url == null || !url.startsWith(prefix)) {
            return null;
        }
        String filename = url.substring(prefix.length());
        if (filename.isBlank() || filename.contains("..")
                || filename.contains("/") || filename.contains("\\")) {
            return null;
        }
        Path file = uploadDir.resolve(filename).normalize();
        return file.startsWith(uploadDir) ? file : null;
    }

    private String extensionOf(String originalFilename) {
        if (originalFilename == null || !originalFilename.contains(".")) {
            throw BusinessException.of(ErrorCode.BAD_PARAM, "缺少文件后缀");
        }
        return originalFilename.substring(originalFilename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }
}
