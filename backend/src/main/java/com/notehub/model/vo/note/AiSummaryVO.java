package com.notehub.model.vo.note;

import lombok.Data;

/**
 * 笔记 AI 总结
 */
@Data
public class AiSummaryVO {

    private Long noteId;

    private String summary;

    /** ai=模型生成 local=未配置/调用失败时的本地摘要 */
    private String source;
}
