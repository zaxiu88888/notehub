package com.notehub.controller;

import com.notehub.common.ApiResponse;
import com.notehub.common.PageVO;
import com.notehub.common.RequestContext;
import com.notehub.model.dto.question.CreateQuestionBatchBody;
import com.notehub.model.dto.question.CreateQuestionBody;
import com.notehub.model.dto.question.QuestionQueryParam;
import com.notehub.model.dto.question.UpdateQuestionBody;
import com.notehub.model.vo.question.BatchImportResultVO;
import com.notehub.model.vo.question.QuestionVO;
import com.notehub.security.NeedAdmin;
import com.notehub.service.QuestionService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 题目 /api/questions (公开读, 管理端写)
 */
@RestController
@RequestMapping("/api")
public class QuestionController {

    private final QuestionService questionService;
    private final RequestContext requestContext;

    public QuestionController(QuestionService questionService, RequestContext requestContext) {
        this.questionService = questionService;
        this.requestContext = requestContext;
    }

    @GetMapping("/questions")
    public ApiResponse<PageVO<QuestionVO>> list(QuestionQueryParam param) {
        return ApiResponse.ok(questionService.listQuestions(param));
    }

    @GetMapping("/questions/{questionId}")
    public ApiResponse<QuestionVO> detail(@PathVariable Long questionId) {
        Long currentUserId = requestContext.isLogin() ? requestContext.getUserId() : null;
        return ApiResponse.ok(questionService.getQuestionDetail(questionId, currentUserId));
    }

    @PostMapping("/admin/questions")
    @NeedAdmin
    public ApiResponse<QuestionVO> create(@Valid @RequestBody CreateQuestionBody body) {
        return ApiResponse.ok("题目已创建", questionService.create(
                body.getCategoryId(), body.getTitle(), body.getDifficulty(),
                body.getExamPoint(), body.getDescription()));
    }

    @PostMapping("/admin/questions/batch")
    @NeedAdmin
    public ApiResponse<BatchImportResultVO> batchImport(@Valid @RequestBody CreateQuestionBatchBody body) {
        return ApiResponse.ok("批量导入完成", questionService.batchImport(body));
    }

    @PatchMapping("/admin/questions/{questionId}")
    @NeedAdmin
    public ApiResponse<QuestionVO> update(@PathVariable Long questionId,
                                          @Valid @RequestBody UpdateQuestionBody body) {
        return ApiResponse.ok("题目已更新", questionService.update(
                questionId, body.getCategoryId(), body.getTitle(),
                body.getDifficulty(), body.getExamPoint(), body.getDescription()));
    }

    @DeleteMapping("/admin/questions/{questionId}")
    @NeedAdmin
    public ApiResponse<Void> delete(@PathVariable Long questionId) {
        questionService.delete(questionId);
        return ApiResponse.ok();
    }
}
