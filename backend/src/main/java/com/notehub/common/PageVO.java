package com.notehub.common;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/**
 * 统一分页响应: {list, page, pageSize, total}
 */
@Data
@AllArgsConstructor
public class PageVO<T> {

    private List<T> list;
    private int page;
    private int pageSize;
    private long total;

    public static <T> PageVO<T> of(List<T> list, int page, int pageSize, long total) {
        return new PageVO<>(list, page, pageSize, total);
    }
}
