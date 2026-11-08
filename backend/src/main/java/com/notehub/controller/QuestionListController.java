package com.notehub.controller;

import com.notehub.common.ApiResponse;
import com.notehub.model.dto.questionList.CreateQuestionListBody;
import com.notehub.model.dto.questionList.UpdateQuestionListBody;
import com.notehub.model.vo.questionList.QuestionListVO;
import com.notehub.security.NeedAdmin;
import com.notehub.service.QuestionListService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 题单 /api/questionlists (公开读, 管理端写)
 */
@RestController
@RequestMapping("/api")
public class QuestionListController {

    private final QuestionListService questionListService;

    public QuestionListController(QuestionListService questionListService) {
        this.questionListService = questionListService;
    }

    @GetMapping("/questionlists")
    public ApiResponse<List<QuestionListVO>> list() {
        return ApiResponse.ok(questionListService.listQuestionLists());
    }

    @GetMapping("/questionlists/{questionListId}")
    public ApiResponse<QuestionListVO> detail(@PathVariable Long questionListId) {
        return ApiResponse.ok(questionListService.getQuestionList(questionListId));
    }

    @PostMapping("/admin/questionlists")
    @NeedAdmin
    public ApiResponse<QuestionListVO> create(@Valid @RequestBody CreateQuestionListBody body) {
        return ApiResponse.ok("题单已创建",
                questionListService.create(body.getName(), body.getType(), body.getDescription()));
    }

    @PatchMapping("/admin/questionlists/{questionListId}")
    @NeedAdmin
    public ApiResponse<QuestionListVO> update(@PathVariable Long questionListId,
                                              @Valid @RequestBody UpdateQuestionListBody body) {
        return ApiResponse.ok("题单已更新", questionListService.update(
                questionListId, body.getName(), body.getType(), body.getDescription()));
    }

    @DeleteMapping("/admin/questionlists/{questionListId}")
    @NeedAdmin
    public ApiResponse<Void> delete(@PathVariable Long questionListId) {
        questionListService.delete(questionListId);
        return ApiResponse.ok();
    }
}
