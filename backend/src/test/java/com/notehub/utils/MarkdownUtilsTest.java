package com.notehub.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarkdownUtilsTest {

    @Test
    @DisplayName("摘要: 去标题/列表/代码块/链接标记, 保留正文")
    void excerptStripsMarkdown() {
        String markdown = """
                # 两数之和

                - 使用**哈希表**, 一次遍历
                > 时间复杂度 O(n)

                ```java
                int[] a = new int[2];
                ```

                参考[官方题解](https://example.com)
                """;

        String excerpt = MarkdownUtils.excerpt(markdown);

        assertEquals("两数之和 使用哈希表, 一次遍历 时间复杂度 O(n) 参考官方题解", excerpt);
    }

    @Test
    @DisplayName("摘要: 超长截断并加省略号; 空输入返回空串")
    void excerptTruncates() {
        String excerpt = MarkdownUtils.excerpt("一二三四五六", 4);
        assertEquals("一二三四…", excerpt);
        assertEquals("", MarkdownUtils.excerpt(null));
        assertEquals("", MarkdownUtils.excerpt("   "));
    }

    @Test
    @DisplayName("摘要: 图片整体省略")
    void excerptDropsImages() {
        String excerpt = MarkdownUtils.excerpt("前缀 ![图](a.png) 后缀");
        assertTrue(excerpt.contains("前缀"));
        assertTrue(excerpt.contains("后缀"));
        assertTrue(!excerpt.contains("a.png"));
    }
}
