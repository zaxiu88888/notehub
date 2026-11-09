package com.notehub.utils;

/**
 * Markdown 文本处理(排行榜摘要等展示场景; 不做渲染)
 */
public final class MarkdownUtils {

    private static final int DEFAULT_EXCERPT_LENGTH = 120;

    private MarkdownUtils() {
    }

    /**
     * 去 Markdown 标记后取摘要: 代码块整体省略, 图片省略, 链接保留文字
     */
    public static String excerpt(String markdown) {
        return excerpt(markdown, DEFAULT_EXCERPT_LENGTH);
    }

    public static String excerpt(String markdown, int maxLength) {
        if (markdown == null || markdown.isBlank()) {
            return "";
        }
        String text = markdown
                .replaceAll("(?s)```.*?```", " ")                       // 代码块
                .replaceAll("!\\[[^]]*]\\([^)]*\\)", " ")               // 图片
                .replaceAll("\\[([^]]*)]\\([^)]*\\)", "$1")             // 链接 → 文字
                .replaceAll("(?m)^\\s{0,3}#{1,6}\\s*", "")              // 标题
                .replaceAll("(?m)^\\s*>\\s?", "")                       // 引用
                .replaceAll("(?m)^\\s*[-*+]\\s+", "")                   // 无序列表
                .replaceAll("[`*_~]", "")                               // 行内标记
                .replaceAll("\\s+", " ")
                .trim();
        if (maxLength <= 0 || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "…";
    }
}
