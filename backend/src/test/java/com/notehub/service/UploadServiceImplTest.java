package com.notehub.service;

import com.notehub.common.BusinessException;
import com.notehub.common.ErrorCode;
import com.notehub.service.impl.UploadServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UploadServiceImplTest {

    @TempDir
    Path tempDir;

    private UploadServiceImpl uploadService;

    @BeforeEach
    void setUp() {
        uploadService = new UploadServiceImpl(tempDir.toString(), "/images");
    }

    private MockMultipartFile file(String name, long size) {
        byte[] bytes = new byte[(int) size];
        return new MockMultipartFile("file", name, "application/octet-stream", bytes);
    }

    @Test
    @DisplayName("合法图片: UUID 重命名写入磁盘, 返回 /images/xxx.ext")
    void savesWithUuidName() throws Exception {
        String url = uploadService.saveImage(file("avatar.PNG", 128));

        assertTrue(url.startsWith("/images/"));
        String filename = url.substring("/images/".length());
        assertTrue(Pattern.matches("[0-9a-f]{32}\\.png", filename)); // 32 位 uuid + 小写后缀
        assertTrue(Files.exists(tempDir.resolve(filename))); // 落盘
    }

    @Test
    @DisplayName("后缀白名单: svg/exe/无后缀/txt 全部拒绝")
    void rejectsNonWhitelistedExtensions() {
        assertEquals(ErrorCode.BAD_PARAM,
                assertThrows(BusinessException.class, () -> uploadService.saveImage(file("x.svg", 10))).getErrorCode());
        assertEquals(ErrorCode.BAD_PARAM,
                assertThrows(BusinessException.class, () -> uploadService.saveImage(file("x.exe", 10))).getErrorCode());
        assertEquals(ErrorCode.BAD_PARAM,
                assertThrows(BusinessException.class, () -> uploadService.saveImage(file("noext", 10))).getErrorCode());
    }

    @Test
    @DisplayName("大小限制: 超 5MB 拒绝; 恰好 5MB 通过")
    void sizeLimit() {
        assertEquals(ErrorCode.BAD_PARAM,
                assertThrows(BusinessException.class,
                        () -> uploadService.saveImage(file("big.jpg", 5 * 1024 * 1024 + 1))).getErrorCode());

        String url = uploadService.saveImage(file("ok.jpg", 5 * 1024 * 1024));
        assertTrue(url.startsWith("/images/"));
    }

    @Test
    @DisplayName("空文件拒绝")
    void rejectsEmpty() {
        assertEquals(ErrorCode.BAD_PARAM,
                assertThrows(BusinessException.class, () -> uploadService.saveImage(file("empty.png", 0))).getErrorCode());
    }

    @Test
    @DisplayName("路径穿越文件名: 只取末尾后缀, 不保留目录部分")
    void pathTraversalNameSanitized() throws Exception {
        String url = uploadService.saveImage(file("../../evil.png", 10));
        String filename = url.substring("/images/".length());
        assertTrue(Pattern.matches("[0-9a-f]{32}\\.png", filename));
        assertTrue(Files.exists(tempDir.resolve(filename)));
    }
}
