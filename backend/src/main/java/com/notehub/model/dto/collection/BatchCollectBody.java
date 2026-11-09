package com.notehub.model.dto.collection;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 批量收藏笔记到收藏夹
 */
@Data
public class BatchCollectBody {

    @NotNull(message = "collectionId 不能为空")
    private Long collectionId;

    @NotEmpty(message = "noteIds 不能为空")
    private List<Long> noteIds;
}
