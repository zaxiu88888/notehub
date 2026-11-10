package com.notehub.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalSummarizerTest {

    @Test
    @DisplayName("抽取式摘要: 去代码块, 优先关键句")
    void picksKeySentences() {
        String markdown = """
                # 两数之和
                ```java
                int x = 1;
                ```
                使用哈希表一次遍历，时间复杂度 O(n)。
                注意边界条件。
                """;

        String summary = LocalSummarizer.summarize(markdown, 200);

        assertTrue(summary.contains("哈希表"));
        assertTrue(summary.contains("复杂度"));
        assertFalse(summary.contains("int x"));
    }

    @Test
    @DisplayName("截断与空内容")
    void truncatesAndHandlesBlank() {
        assertTrue(LocalSummarizer.summarize("一二三四五六七八九十", 4).endsWith("…"));
        assertEquals("", LocalSummarizer.summarize("   ", 100));
        assertEquals("", LocalSummarizer.summarize(null, 100));
    }
}
