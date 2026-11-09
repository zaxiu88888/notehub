package com.notehub.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * 文件上传服务(本地磁盘)
 */
public interface UploadService {

    /**
     * 保存图片并返回可直接访问的相对 URL(/images/xxx.png)
     * 校验: 后缀白名单 + 大小上限 + 空文件; 文件名用 UUID 重写防路径穿越/同名覆盖
     */
    String saveImage(MultipartFile file);
}
