package com.notehub.utils;

import com.huaban.analysis.jieba.JiebaSegmenter;
import com.huaban.analysis.jieba.SegToken;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 搜索关键词预处理(Jieba 分词)
 * 1) 去掉 Markdown 标记与标点符号
 * 2) 分词后去重(保持顺序)
 * 3) 交给 MySQL ngram 全文索引匹配(中文 bigram, 与分词互补)
 */
public final class SearchUtils {

    private static final JiebaSegmenter SEGMENTER = new JiebaSegmenter();
    private static final int MAX_KEYWORD_LENGTH = 64;

    private SearchUtils() {
    }

    /**
     * 布尔短语查询: 每个分词加双引号, 避免 ngram 只命中跨词 bigram 的低分噪声。
     * 例: "哈希表 数组" → "\"哈希表\" \"数组\"" (OR 语义, 保持自然语言式的多词召回)
     */
    public static String toBooleanQuery(String processedKeyword) {
        if (processedKeyword == null || processedKeyword.isBlank()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (String word : processedKeyword.trim().split("\\s+")) {
            if (word.isBlank()) {
                continue;
            }
            if (!sb.isEmpty()) {
                sb.append(' ');
            }
            sb.append('"').append(word.replace("\"", "")).append('"');
        }
        return sb.toString();
    }

    /**
     * @return 预处理后的关键词(空格分隔), 空白输入返回空串
     */
    public static String preprocessKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return "";
        }
        // 去 Markdown 符号与中英文标点
        String cleaned = keyword
                .replaceAll("[`#>*_\\[\\]()~]", " ")
                .replaceAll("[\\p{P}\\p{S}]", " ")
                .trim();
        if (cleaned.isEmpty()) {
            return "";
        }
        Set<String> tokens = new LinkedHashSet<>();
        for (SegToken token : SEGMENTER.process(cleaned, JiebaSegmenter.SegMode.SEARCH)) {
            String word = token.word.trim();
            if (!word.isEmpty()) {
                tokens.add(word);
            }
            if (tokens.size() >= 8) { // 控制查询长度, 避免超长 MATCH
                break;
            }
        }
        String result = String.join(" ", tokens);
        return result.length() > MAX_KEYWORD_LENGTH ? result.substring(0, MAX_KEYWORD_LENGTH) : result;
    }
}
