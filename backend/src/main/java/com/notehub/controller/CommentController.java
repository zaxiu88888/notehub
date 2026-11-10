package com.notehub.controller;

import com.notehub.common.ApiResponse;
import com.notehub.common.PageVO;
import com.notehub.common.RateLimit;
import com.notehub.common.RequestContext;
import com.notehub.model.dto.comment.CommentContentBody;
import com.notehub.model.dto.comment.CommentUpdateBody;
import com.notehub.model.vo.comment.CommentVO;
import com.notehub.security.NeedLogin;
import com.notehub.service.CommentQueryService;
import com.notehub.service.NoteInteractionService;
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

import java.util.Map;

/**
 * 评论 /api/comments
 */
@RestController
@RequestMapping("/api")
@Validated
public class CommentController {

    private final NoteInteractionService interactionService;
    private final CommentQueryService commentQueryService;
    private final RequestContext requestContext;

    public CommentController(NoteInteractionService interactionService,
                             CommentQueryService commentQueryService,
                             RequestContext requestContext) {
        this.interactionService = interactionService;
        this.commentQueryService = commentQueryService;
        this.requestContext = requestContext;
    }

    @GetMapping("/comments")
    public ApiResponse<PageVO<CommentVO>> list(
            @RequestParam @NotNull(message = "noteId 不能为空") Long noteId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        Long currentUserId = requestContext.isLogin() ? requestContext.getUserId() : null;
        return ApiResponse.ok(commentQueryService.listComments(noteId, page, pageSize, currentUserId));
    }

    @PostMapping("/comments")
    @NeedLogin
    @RateLimit(key = "comment:create", limit = 20)
    public ApiResponse<Map<String, Long>> create(@Valid @RequestBody CommentContentBody body) {
        Long commentId = interactionService.addComment(body.getNoteId(),
                requestContext.requireUserId(), body.getParentId(), body.getContent());
        return ApiResponse.ok("评论成功", Map.of("commentId", commentId));
    }

    @PatchMapping("/comments/{commentId}")
    @NeedLogin
    public ApiResponse<Void> update(@PathVariable Long commentId,
                                    @Valid @RequestBody CommentUpdateBody body) {
        interactionService.updateComment(commentId, requestContext.requireUserId(), body.getContent());
        return ApiResponse.ok();
    }

    @DeleteMapping("/comments/{commentId}")
    @NeedLogin
    public ApiResponse<Void> delete(@PathVariable Long commentId) {
        interactionService.deleteComment(commentId, requestContext.requireUserId());
        return ApiResponse.ok();
    }
}
