package com.notehub.utils;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 题目批量导入的 Markdown 解析器 (docs/api-contract.md §7 模板)
 *
 * 模板规则:
 *   # 标题            → 题目标题
 *   难度：简单|中等|困难 → difficulty 1/2/3 (必填)
 *   考点：a；b；c      → exam_point (可选, 中文分号或英文分号)
 *   其余行             → 题干 description (可选)
 *
 * 设计说明: 模板是固定行协议, 行级解析比 Flexmark AST 更可控可测;
 * Flexmark 预留给后续笔记摘要/导出场景 (技术设计 §5.4 已同步)
 */
public final class QuestionMarkdownParser {

    private QuestionMarkdownParser() {
    }

    @Data
    @AllArgsConstructor
    public static class ParsedQuestion {
        private String title;
        private Integer difficulty;   // 1/2/3, 解析失败为 null
        private String examPoint;
        private String description;
    }

    /** 解析失败: 标题 + 原因 */
    @Data
    @AllArgsConstructor
    public static class ParseError {
        private String title;
        private String reason;
    }

    @Data
    public static class ParseResult {
        private List<ParsedQuestion> questions = new ArrayList<>();
        private List<ParseError> errors = new ArrayList<>();
    }

    private static final String[] DIFFICULTY_KEYS = {"难度：", "难度:"};
    private static final String[] EXAM_POINT_KEYS = {"考点：", "考点:"};
    private static final String HEADING_PREFIX = "# ";

    /** 标题行: "# xxx" 或裸 "#"(空标题, 会报错); "## xx" 是正文不是切块 */
    private static boolean isHeading(String line) {
        return line.startsWith(HEADING_PREFIX) || line.equals("#");
    }

    public static ParseResult parse(String markdown) {
        ParseResult result = new ParseResult();
        if (markdown == null || markdown.isBlank()) {
            result.getErrors().add(new ParseError("", "markdown 为空"));
            return result;
        }

        for (String block : splitBlocks(markdown)) {
            parseBlock(block, result);
        }
        return result;
    }

    /** 按 "# " 标题行切块 */
    private static List<String> splitBlocks(String markdown) {
        List<String> blocks = new ArrayList<>();
        StringBuilder current = null;
        for (String line : markdown.split("\\r?\\n")) {
            if (isHeading(line)) {
                if (current != null) {
                    blocks.add(current.toString());
                }
                current = new StringBuilder(line).append('\n');
            } else if (current != null) {
                current.append(line).append('\n');
            }
        }
        if (current != null) {
            blocks.add(current.toString());
        }
        return blocks;
    }

    private static void parseBlock(String block, ParseResult result) {
        String[] lines = block.split("\\r?\\n");
        // 兼容 "#", "# ", "# 标题" 各种形态, 统一剥离前导 # 与空白
        String title = lines[0].replaceFirst("^#+\\s*", "").trim();
        if (title.isEmpty()) {
            result.getErrors().add(new ParseError("", "题目标题为空"));
            return;
        }

        Integer difficulty = null;
        String examPoint = null;
        StringBuilder description = new StringBuilder();

        for (int i = 1; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty()) {
                continue;
            }
            Integer d = matchDifficulty(line);
            if (d != null) {
                difficulty = d;
                continue;
            }
            String exam = matchExamPoint(line);
            if (exam != null) {
                examPoint = exam;
                continue;
            }
            if (description.length() > 0) {
                description.append('\n');
            }
            description.append(line);
        }

        if (difficulty == null) {
            result.getErrors().add(new ParseError(title, "缺少「难度：」行"));
            return;
        }
        result.getQuestions().add(new ParsedQuestion(title, difficulty, examPoint, description.toString()));
    }

    private static Integer matchDifficulty(String line) {
        for (String key : DIFFICULTY_KEYS) {
            if (line.startsWith(key)) {
                String value = line.substring(key.length()).trim();
                return switch (value) {
                    case "简单" -> 1;
                    case "中等" -> 2;
                    case "困难" -> 3;
                    default -> null; // 无法识别 → 视为普通文本行
                };
            }
        }
        return null;
    }

    private static String matchExamPoint(String line) {
        for (String key : EXAM_POINT_KEYS) {
            if (line.startsWith(key)) {
                return line.substring(key.length()).trim();
            }
        }
        return null;
    }
}
