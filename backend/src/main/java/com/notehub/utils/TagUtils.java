package com.notehub.utils;

import com.notehub.common.BusinessException;
import com.notehub.common.ErrorCode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 标签规范化: 去空白/#前缀, 大小写不敏感去重, 限制数量与长度
 */
public final class TagUtils {

    private static final int MAX_NAME_LENGTH = 20;

    private TagUtils() {
    }

    public static List<String> normalize(List<String> raw, int maxCount) {
        if (raw == null || raw.isEmpty()) {
            return List.of();
        }
        Map<String, String> unique = new LinkedHashMap<>();
        for (String value : raw) {
            if (value == null) {
                continue;
            }
            String name = value.trim().replaceFirst("^#+", "").trim();
            if (name.isEmpty()) {
                continue;
            }
            if (name.length() > MAX_NAME_LENGTH) {
                throw BusinessException.of(ErrorCode.BAD_PARAM, "标签最长 " + MAX_NAME_LENGTH + " 个字符");
            }
            unique.putIfAbsent(name.toLowerCase(Locale.ROOT), name);
        }
        if (unique.size() > maxCount) {
            throw BusinessException.of(ErrorCode.BAD_PARAM, "最多 " + maxCount + " 个标签");
        }
        return new ArrayList<>(unique.values());
    }

    /** 题干考点(分号分隔, 兼容中英文分号) → 题目标签 */
    public static List<String> splitExamPoints(String examPoint) {
        if (examPoint == null || examPoint.isBlank()) {
            return List.of();
        }
        return normalize(Arrays.asList(examPoint.split("[;；]")), 10);
    }
}
