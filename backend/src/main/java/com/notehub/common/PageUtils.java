package com.notehub.common;

/**
 * 分页参数统一处理
 * 1) 页码/页大小钳制到安全范围(防整数溢出产生负 OFFSET)
 * 2) 上限 100 万页: 与最大 pageSize 相乘仍在 int 范围内
 */
public final class PageUtils {

    public static final int MAX_PAGE = 1_000_000;
    public static final int MAX_PAGE_SIZE = 50;

    private PageUtils() {
    }

    public static int clampPage(int page) {
        return Math.min(Math.max(page, 1), MAX_PAGE);
    }

    public static int clampPageSize(int pageSize) {
        return Math.min(Math.max(pageSize, 1), MAX_PAGE_SIZE);
    }

    public static int offset(int clampedPage, int clampedPageSize) {
        return (clampedPage - 1) * clampedPageSize;
    }
}
