package com.notehub.model.dto.questionListItem;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 题单添加条目(管理端): 不传 sortRank 则追加到末尾
 */
@Data
public class CreateQuestionListItemBody {

    @NotNull(message = "questionListId 不能为空")
    private Long questionListId;

    @NotNull(message = "questionId 不能为空")
    private Long questionId;

    private Integer sortRank;
}
