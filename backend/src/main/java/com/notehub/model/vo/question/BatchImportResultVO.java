package com.notehub.model.vo.question;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/**
 * Markdown 批量导入结果
 */
@Data
@AllArgsConstructor
public class BatchImportResultVO {

    private int total;

    private int success;

    private List<Failure> failures;

    @Data
    @AllArgsConstructor
    public static class Failure {

        private String title;

        private String reason;
    }
}
