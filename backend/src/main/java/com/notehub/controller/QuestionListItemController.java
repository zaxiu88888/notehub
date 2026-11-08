package com.notehub.controller;

import com.notehub.common.ApiResponse;
import com.notehub.common.RequestContext;
import com.notehub.model.dto.questionListItem.CreateQuestionListItemBody;
import com.notehub.model.dto.questionListItem.SortQuestionListItemBody;
import com.notehub.model.vo.questionListItem.QuestionListItemVO;
import com.notehub.security.NeedAdmin;
import com.notehub.security.NeedLogin;
import com.notehub.service.QuestionListService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 题单条目 /api/questionlist-items (用户端读进度, 管理端写)
 */
@RestController
@RequestMapping("/api")
@Validated
public class QuestionListItemController {

    private final QuestionListService questionListService;
    private final RequestContext requestContext;

    public QuestionListItemController(QuestionListService questionListService,
                                      RequestContext requestContext) {
        this.questionListService = questionListService;
        this.requestContext = requestContext;
    }

    @GetMapping("/questionlist-items")
    @NeedLogin
    public ApiResponse<List<QuestionListItemVO>> listItems(
            @RequestParam @NotNull(message = "questionListId 不能为空") Long questionListId) {
        return ApiResponse.ok(
                questionListService.listItems(questionListId, requestContext.getUserId()));
    }

    @GetMapping("/admin/questionlist-items/{questionListId}")
    @NeedAdmin
    public ApiResponse<List<QuestionListItemVO>> listItemsForAdmin(
            @PathVariable Long questionListId) {
        return ApiResponse.ok(questionListService.listItemsForAdmin(questionListId));
    }

    @PostMapping("/admin/questionlist-items")
    @NeedAdmin
    public ApiResponse<Void> addItem(@Valid @RequestBody CreateQuestionListItemBody body) {
        questionListService.addItem(body.getQuestionListId(), body.getQuestionId(), body.getSortRank());
        return ApiResponse.ok();
    }

    @DeleteMapping("/admin/questionlist-items/{questionListId}/{questionId}")
    @NeedAdmin
    public ApiResponse<Void> removeItem(@PathVariable Long questionListId,
                                        @PathVariable Long questionId) {
        questionListService.removeItem(questionListId, questionId);
        return ApiResponse.ok();
    }

    @PatchMapping("/admin/questionlist-items/sort")
    @NeedAdmin
    public ApiResponse<Void> sort(@Valid @RequestBody SortQuestionListItemBody body) {
        questionListService.sortItems(body.getQuestionListId(),
                body.getItems().stream().map(SortQuestionListItemBody.Item::getQuestionId).toList());
        return ApiResponse.ok();
    }
}
