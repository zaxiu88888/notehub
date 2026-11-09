package com.notehub.model.vo.upload;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 图片上传结果
 */
@Data
@AllArgsConstructor
public class ImageVO {

    /** 可直接访问的相对路径: /images/xxx.png */
    private String url;
}
