package com.notehub.controller;

import com.notehub.common.ApiResponse;
import com.notehub.model.vo.upload.ImageVO;
import com.notehub.security.NeedLogin;
import com.notehub.service.UploadService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 图片上传 /api/upload/image
 */
@RestController
@RequestMapping("/api")
public class UploadController {

    private final UploadService uploadService;

    public UploadController(UploadService uploadService) {
        this.uploadService = uploadService;
    }

    @PostMapping("/upload/image")
    @NeedLogin
    public ApiResponse<ImageVO> uploadImage(@RequestPart("file") MultipartFile file) {
        return ApiResponse.ok(new ImageVO(uploadService.saveImage(file)));
    }
}
