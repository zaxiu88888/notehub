package com.notehub.utils;

import com.notehub.common.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TagUtilsTest {

    @Test
    @DisplayName("规范化: 去空白/#前缀, 大小写不敏感去重, 忽略空值")
    void normalizeTags() {
        List<String> tags = TagUtils.normalize(
                List.of("  数组 ", "#哈希表", "dp", "DP", "", "  "), 5);

        assertEquals(List.of("数组", "哈希表", "dp"), tags);
    }

    @Test
    @DisplayName("规范化: 超长/超量 → 40001")
    void normalizeLimits() {
        assertThrows(BusinessException.class,
                () -> TagUtils.normalize(List.of("a".repeat(21)), 5));
        assertThrows(BusinessException.class,
                () -> TagUtils.normalize(List.of("a", "b", "c", "d", "e", "f"), 5));
    }

    @Test
    @DisplayName("考点拆分: 中英文分号, 去重")
    void splitExamPoints() {
        assertEquals(List.of("数组", "哈希表", "双指针"),
                TagUtils.splitExamPoints("数组；哈希表;双指针;数组"));
        assertEquals(List.of(), TagUtils.splitExamPoints("  "));
        assertEquals(List.of(), TagUtils.splitExamPoints(null));
    }
}
