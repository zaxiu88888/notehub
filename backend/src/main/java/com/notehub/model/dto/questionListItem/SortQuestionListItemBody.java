package com.notehub.model.dto.questionListItem;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 题单条目批量排序(管理端): items 顺序即 sort_rank=1..n
 */
@Data
public class SortQuestionListItemBody {

    @NotNull(message = "questionListId 不能为空")
    private Long questionListId;

    @NotEmpty(message = "items 不能为空")
    @Valid
    private List<Item> items;

    @Data
    public static class Item {

        @NotNull(message = "questionId 不能为空")
        private Long questionId;
    }
}
