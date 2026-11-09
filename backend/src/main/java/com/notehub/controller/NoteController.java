package com.notehub.controller;

import com.notehub.common.ApiResponse;
import com.notehub.common.PageVO;
import com.notehub.common.RequestContext;
import com.notehub.model.dto.note.NoteContentBody;
import com.notehub.model.dto.note.NoteQueryParams;
import com.notehub.model.dto.note.NoteUpdateBody;
import com.notehub.model.vo.note.NoteVO;
import com.notehub.security.NeedLogin;
import com.notehub.service.NoteService;
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
 * 笔记 /api/notes
 */
@RestController
@RequestMapping("/api")
@Validated
public class NoteController {

    private final NoteService noteService;
    private final RequestContext requestContext;

    public NoteController(NoteService noteService, RequestContext requestContext) {
        this.noteService = noteService;
        this.requestContext = requestContext;
    }

    @GetMapping("/notes")
    public ApiResponse<PageVO<NoteVO>> list(NoteQueryParams params) {
        Long currentUserId = requestContext.isLogin() ? requestContext.getUserId() : null;
        return ApiResponse.ok(noteService.listNotes(params, currentUserId));
    }

    @GetMapping("/notes/{noteId}")
    public ApiResponse<NoteVO> detail(@PathVariable Long noteId) {
        Long currentUserId = requestContext.isLogin() ? requestContext.getUserId() : null;
        return ApiResponse.ok(noteService.getNote(noteId, currentUserId));
    }

    @PostMapping("/notes")
    @NeedLogin
    public ApiResponse<Map<String, Long>> create(@Valid @RequestBody NoteContentBody body) {
        Long noteId = noteService.createNote(requestContext.requireUserId(),
                body.getQuestionId(), body.getContent());
        return ApiResponse.ok("发布成功", Map.of("noteId", noteId));
    }

    @PatchMapping("/notes/{noteId}")
    @NeedLogin
    public ApiResponse<Void> update(@PathVariable Long noteId,
                                    @Valid @RequestBody NoteUpdateBody body) {
        noteService.updateNote(noteId, requestContext.requireUserId(), body.getContent());
        return ApiResponse.ok();
    }

    @DeleteMapping("/notes/{noteId}")
    @NeedLogin
    public ApiResponse<Void> delete(@PathVariable Long noteId) {
        noteService.deleteNote(noteId, requestContext.requireUserId());
        return ApiResponse.ok();
    }

    /** 我在某题下的笔记(题目详情页"编辑我的笔记"入口) */
    @GetMapping("/notes/mine")
    @NeedLogin
    public ApiResponse<Map<String, Long>> myNote(
            @RequestParam @NotNull(message = "questionId 不能为空") Long questionId) {
        Long noteId = noteService.findMyNoteId(requestContext.requireUserId(), questionId);
        return ApiResponse.ok(Map.of("noteId", noteId == null ? -1 : noteId));
    }
}
