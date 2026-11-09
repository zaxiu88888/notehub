package com.notehub.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SearchUtilsTest {

    @Test
    @DisplayName("中文分词: 生成空格分隔的关键词")
    void segmentsChinese() {
        String result = SearchUtils.preprocessKeyword("哈希表两数之和");
        assertFalse(result.isBlank());
        assertFalse(result.contains("哈希表两数之和"), "应被分词而不是原样");
    }

    @Test
    @DisplayName("去除 Markdown 符号与标点")
    void stripsMarkdownAndPunctuation() {
        String result = SearchUtils.preprocessKeyword("**哈希表**，`complement`！");
        assertFalse(result.contains("*"));
        assertFalse(result.contains("`"));
        assertFalse(result.contains("，"));
        assertTrue(result.contains("complement"), "英文词应保留: " + result);
    }

    @Test
    @DisplayName("空白/纯符号输入 → 空串")
    void blankInput() {
        assertEquals("", SearchUtils.preprocessKeyword(null));
        assertEquals("", SearchUtils.preprocessKeyword("   "));
        assertEquals("", SearchUtils.preprocessKeyword("!!!***"));
    }

    @Test
    @DisplayName("关键词长度受限(防超长 MATCH)")
    void capsLength() {
        String longKeyword = "动态规划滑动窗口二分查找哈希表二叉树".repeat(10);
        assertTrue(SearchUtils.preprocessKeyword(longKeyword).length() <= 64);
    }
}
