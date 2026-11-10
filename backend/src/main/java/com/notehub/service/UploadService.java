package com.notehub.service;

import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.util.List;

/**
 * 文件上传服务(本地磁盘)
 */
public interface UploadService {

    /**
     * 保存图片并返回可直接访问的相对 URL(/images/xxx.png)
     * 校验: 后缀白名单 + 大小上限 + 空文件; 文件名用 UUID 重写防路径穿越/同名覆盖
     */
    String saveImage(MultipartFile file);

    /** 删除本地图片(仅限 /images/ 下的扁平文件名; 越界/非本服务 URL 返回 false) */
    boolean deleteByUrl(String url);

    /** 列出早于 minAge 的已上传图片 URL(供孤儿清理, 避免误删刚上传未引用的图) */
    List<String> listStoredUrls(Duration minAge);
}
