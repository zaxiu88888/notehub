package com.notehub.utils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 本地抽取式摘要(未配置 AI 时的降级):
 * 去 Markdown → 分句 → 关键词/长度打分 → 按原文顺序取 3 句
 */
public final class LocalSummarizer {

    private static final Set<String> KEYWORDS = Set.of(
            "思路", "复杂度", "遍历", "哈希", "指针", "递归", "动态规划",
            "注意", "关键", "优化", "解法", "因为", "所以");

    private LocalSummarizer() {
    }

    public static String summarize(String markdown, int maxChars) {
        String plain = MarkdownUtils.excerpt(markdown, 0);
        if (plain.isBlank()) {
            return "";
        }
        List<String> sentences = new ArrayList<>();
        for (String raw : plain.split("(?<=[。！？!?；;])|\\n+")) {
            String sentence = raw.trim();
            if (sentence.length() >= 4) {
                sentences.add(sentence);
            }
        }
        if (sentences.isEmpty()) {
            return truncate(plain, maxChars);
        }

        record Scored(int index, String sentence, int score) {
        }
        List<Scored> scored = new ArrayList<>();
        for (int i = 0; i < sentences.size(); i++) {
            String sentence = sentences.get(i);
            int score = 0;
            for (String keyword : KEYWORDS) {
                if (sentence.contains(keyword)) {
                    score += 2;
                }
            }
            if (sentence.length() >= 12) {
                score += 1;
            }
            scored.add(new Scored(i, sentence, score));
        }
        List<Scored> top = scored.stream()
                .sorted(Comparator.comparingInt(Scored::score).reversed()
                        .thenComparingInt(Scored::index))
                .limit(3)
                .sorted(Comparator.comparingInt(Scored::index))
                .toList();

        Set<String> picked = new LinkedHashSet<>();
        top.forEach(item -> picked.add(item.sentence()));
        String summary = String.join("；", picked);
        return truncate(summary, maxChars);
    }

    private static String truncate(String text, int maxChars) {
        if (maxChars <= 0 || text.length() <= maxChars) {
            return text;
        }
        return text.substring(0, maxChars) + "…";
    }
}
