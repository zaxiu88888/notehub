package com.notehub.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PageUtilsTest {

    @Test
    @DisplayName("页码钳制: 0/负数→1, 超大→上限(防 int 溢出)")
    void clampPage() {
        assertEquals(1, PageUtils.clampPage(0));
        assertEquals(1, PageUtils.clampPage(-5));
        assertEquals(3, PageUtils.clampPage(3));
        assertEquals(PageUtils.MAX_PAGE, PageUtils.clampPage(Integer.MAX_VALUE));
    }

    @Test
    @DisplayName("页大小钳制: 1~50")
    void clampPageSize() {
        assertEquals(1, PageUtils.clampPageSize(0));
        assertEquals(50, PageUtils.clampPageSize(9999));
        assertEquals(10, PageUtils.clampPageSize(10));
    }

    @Test
    @DisplayName("offset 永不溢出为负")
    void offsetNeverNegative() {
        int offset = PageUtils.offset(PageUtils.clampPage(Integer.MAX_VALUE), 50);
        assertEquals((PageUtils.MAX_PAGE - 1) * 50, offset);
        assertEquals(0, PageUtils.offset(1, 50));
    }
}
