package com.notehub.controller;

import com.notehub.common.ApiResponse;
import com.notehub.common.RequestContext;
import com.notehub.security.NeedLogin;
import com.notehub.service.NoteInteractionService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 点赞 /api/like/**
 */
@RestController
@RequestMapping("/api")
public class LikeController {

    private final NoteInteractionService interactionService;
    private final RequestContext requestContext;

    public LikeController(NoteInteractionService interactionService, RequestContext requestContext) {
        this.interactionService = interactionService;
        this.requestContext = requestContext;
    }

    @PostMapping("/like/note/{noteId}")
    @NeedLogin
    public ApiResponse<Map<String, Integer>> likeNote(@PathVariable Long noteId) {
        int likeCount = interactionService.likeNote(noteId, requestContext.requireUserId());
        return ApiResponse.ok(Map.of("likeCount", likeCount));
    }

    @DeleteMapping("/like/note/{noteId}")
    @NeedLogin
    public ApiResponse<Void> unlikeNote(@PathVariable Long noteId) {
        interactionService.unlikeNote(noteId, requestContext.requireUserId());
        return ApiResponse.ok();
    }

    @PostMapping("/like/comment/{commentId}")
    @NeedLogin
    public ApiResponse<Void> likeComment(@PathVariable Long commentId) {
        interactionService.likeComment(commentId, requestContext.requireUserId());
        return ApiResponse.ok();
    }

    @DeleteMapping("/like/comment/{commentId}")
    @NeedLogin
    public ApiResponse<Void> unlikeComment(@PathVariable Long commentId) {
        interactionService.unlikeComment(commentId, requestContext.requireUserId());
        return ApiResponse.ok();
    }
}
