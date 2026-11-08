package com.notehub.utils;

import com.notehub.utils.QuestionMarkdownParser.ParseResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuestionMarkdownParserTest {

    private static final String TEMPLATE = """
            # 两数之和
            难度：简单
            考点：数组；哈希表
            给定整数数组 nums 和目标值 target。
            返回下标。

            # 无重复字符的最长子串
            难度:中等
            考点:滑动窗口
            求最长无重复子串长度。

            # 爬楼梯
            难度：简单
            每次可以爬 1 或 2 个台阶。
            """;

    @Test
    @DisplayName("标准模板: 标题/难度/考点/题干全解析, 中文英文冒号与分号都兼容")
    void parseStandardTemplate() {
        ParseResult result = QuestionMarkdownParser.parse(TEMPLATE);

        assertEquals(3, result.getQuestions().size());
        assertTrue(result.getErrors().isEmpty());

        var first = result.getQuestions().get(0);
        assertEquals("两数之和", first.getTitle());
        assertEquals(1, first.getDifficulty());
        assertEquals("数组；哈希表", first.getExamPoint());
        assertTrue(first.getDescription().contains("给定整数数组 nums"));

        var second = result.getQuestions().get(1);
        assertEquals(2, second.getDifficulty()); // 英文冒号
        assertEquals("滑动窗口", second.getExamPoint());

        var third = result.getQuestions().get(2);
        assertEquals(1, third.getDifficulty());
        assertNull(third.getExamPoint()); // 缺考点行是允许的
        assertEquals("每次可以爬 1 或 2 个台阶。", third.getDescription());
    }

    @Test
    @DisplayName("缺「难度：」行 → 记入 errors, 不入库")
    void missingDifficultyLineGoesToErrors() {
        ParseResult result = QuestionMarkdownParser.parse("""
                # 无难度的题
                只有题干。
                """);

        assertEquals(0, result.getQuestions().size());
        assertEquals(1, result.getErrors().size());
        assertEquals("无难度的题", result.getErrors().get(0).getTitle());
    }

    @Test
    @DisplayName("难度值不合法(拼写错) → 当作普通文本, 随后仍因缺难度报错")
    void invalidDifficultyValueTreatedAsText() {
        ParseResult result = QuestionMarkdownParser.parse("""
                # 奇怪的题
                难度：超难
                """);

        assertEquals(0, result.getQuestions().size());
        assertEquals(1, result.getErrors().size());
    }

    @Test
    @DisplayName("标题行为空 → 报错; 空 markdown → 报错")
    void emptyTitleAndEmptyInput() {
        ParseResult r1 = QuestionMarkdownParser.parse("#\n难度：简单\n");
        assertEquals(0, r1.getQuestions().size());
        assertEquals(1, r1.getErrors().size());

        ParseResult r2 = QuestionMarkdownParser.parse("   ");
        assertEquals(1, r2.getErrors().size());
        assertTrue(r2.getQuestions().isEmpty());
    }

    @Test
    @DisplayName("正文中出现 # 井号开头的 Markdown 不切块(仅 '# ' 行切块)")
    void contentWithHashIsKept() {
        ParseResult result = QuestionMarkdownParser.parse("""
                # 代码题
                难度：简单
                ## 小节标题
                正文包含 ## 小节。
                """);

        assertEquals(1, result.getQuestions().size());
        assertTrue(result.getQuestions().get(0).getDescription().contains("## 小节标题"));
    }
}
