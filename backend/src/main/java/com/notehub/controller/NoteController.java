package com.notehub.controller;

import com.notehub.common.ApiResponse;
import com.notehub.common.PageVO;
import com.notehub.common.RateLimit;
import com.notehub.common.RequestContext;
import com.notehub.model.dto.note.NoteContentBody;
import com.notehub.model.dto.note.NoteQueryParams;
import com.notehub.model.dto.note.NoteUpdateBody;
import com.notehub.model.vo.note.AiSummaryVO;
import com.notehub.model.vo.note.HeatmapVO;
import com.notehub.model.vo.note.NoteDailyStatVO;
import com.notehub.model.vo.note.NoteStatsVO;
import com.notehub.model.vo.note.NoteVO;
import com.notehub.model.vo.note.RankNoteVO;
import com.notehub.security.NeedLogin;
import com.notehub.service.AiSummaryService;
import com.notehub.service.NoteAnalyticsService;
import com.notehub.service.NoteDailyStatService;
import com.notehub.service.NoteService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * 笔记 /api/notes
 */
@RestController
@RequestMapping("/api")
@Validated
public class NoteController {

    private final NoteService noteService;
    private final NoteAnalyticsService analyticsService;
    private final NoteDailyStatService noteDailyStatService;
    private final AiSummaryService aiSummaryService;
    private final RequestContext requestContext;

    public NoteController(NoteService noteService, NoteAnalyticsService analyticsService,
                          NoteDailyStatService noteDailyStatService,
                          AiSummaryService aiSummaryService,
                          RequestContext requestContext) {
        this.noteService = noteService;
        this.analyticsService = analyticsService;
        this.noteDailyStatService = noteDailyStatService;
        this.aiSummaryService = aiSummaryService;
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
    @RateLimit(key = "note:create", limit = 10)
    public ApiResponse<Map<String, Long>> create(@Valid @RequestBody NoteContentBody body) {
        Long noteId = noteService.createNote(requestContext.requireUserId(),
                body.getQuestionId(), body.getContent(), body.getTags());
        return ApiResponse.ok("发布成功", Map.of("noteId", noteId));
    }

    @PatchMapping("/notes/{noteId}")
    @NeedLogin
    @RateLimit(key = "note:update", limit = 30)
    public ApiResponse<Void> update(@PathVariable Long noteId,
                                    @Valid @RequestBody NoteUpdateBody body) {
        noteService.updateNote(noteId, requestContext.requireUserId(),
                body.getContent(), body.getTags());
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

    // ---------- I2 数据可视化 ----------

    /** 刷题热力图: 默认今年 */
    @GetMapping("/notes/heatmap")
    @NeedLogin
    public ApiResponse<HeatmapVO> heatmap(
            @RequestParam(required = false) Integer year) {
        return ApiResponse.ok(analyticsService.getHeatmap(requestContext.requireUserId(), year));
    }

    /** 笔记热度榜: period=week|month|all, limit 1..50 */
    @GetMapping("/notes/ranklist")
    public ApiResponse<List<RankNoteVO>> ranklist(
            @RequestParam(defaultValue = "week") String period,
            @RequestParam(required = false) Integer limit) {
        return ApiResponse.ok(analyticsService.getRanklist(period, limit));
    }

    /** AI 题解总结: 未配置模型时返回本地摘要; 结果缓存 6h */
    @PostMapping("/notes/{noteId}/ai-summary")
    @NeedLogin
    @RateLimit(key = "ai:summary", limit = 5)
    public ApiResponse<AiSummaryVO> aiSummary(@PathVariable Long noteId) {
        return ApiResponse.ok(aiSummaryService.summarize(noteId));
    }

    /** 笔记近 N 天互动趋势(每日点赞/评论/收藏, 主键去重) */
    @GetMapping("/notes/{noteId}/stats")
    public ApiResponse<List<NoteDailyStatVO>> dailyStats(
            @PathVariable Long noteId,
            @RequestParam(defaultValue = "7") Integer days) {
        return ApiResponse.ok(noteDailyStatService.recent(noteId, days));
    }

    /** 个人数据卡片: 笔记/获赞/连续提交等 */
    @GetMapping("/notes/top3count")
    @NeedLogin
    public ApiResponse<NoteStatsVO> topStats() {
        return ApiResponse.ok(analyticsService.getTopStats(requestContext.requireUserId()));
    }

    /** 导出笔记为 Markdown 附件: 带 noteId 导单篇, 不带导出全部 */
    @GetMapping("/notes/download")
    @NeedLogin
    public ResponseEntity<byte[]> download(@RequestParam(required = false) Long noteId) {
        byte[] content = analyticsService.exportNotes(requestContext.requireUserId(), noteId);
        String filename = noteId == null
                ? "notehub-notes-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + ".md"
                : "note-" + noteId + ".md";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(new MediaType("text", "markdown", StandardCharsets.UTF_8))
                .contentLength(content.length)
                .body(content);
    }
}
