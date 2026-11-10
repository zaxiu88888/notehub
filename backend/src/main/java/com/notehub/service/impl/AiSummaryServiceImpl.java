package com.notehub.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.notehub.common.BusinessException;
import com.notehub.common.ErrorCode;
import com.notehub.mapper.NoteMapper;
import com.notehub.model.entity.Note;
import com.notehub.model.enums.RedisKey;
import com.notehub.model.vo.note.AiSummaryVO;
import com.notehub.service.AiChatClient;
import com.notehub.service.AiSummaryService;
import com.notehub.service.RedisService;
import com.notehub.utils.MarkdownUtils;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Log4j2
@Service
public class AiSummaryServiceImpl implements AiSummaryService {

    private static final Duration CACHE_TTL = Duration.ofHours(6);
    private static final int MAX_CONTENT_CHARS = 6000;
    private static final int LOCAL_SUMMARY_CHARS = 200;
    private static final String SYSTEM_PROMPT = """
            你是刷题社区的题解助手。请用简体中文总结用户题解的解题思路，
            输出 3 条以内的要点，总字数不超过 200 字；只基于给定内容，不要编造。
            """;

    private final NoteMapper noteMapper;
    private final RedisService redisService;
    private final AiChatClient aiChatClient;
    private final ObjectMapper objectMapper;

    public AiSummaryServiceImpl(NoteMapper noteMapper, RedisService redisService,
                                AiChatClient aiChatClient, ObjectMapper objectMapper) {
        this.noteMapper = noteMapper;
        this.redisService = redisService;
        this.aiChatClient = aiChatClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public AiSummaryVO summarize(Long noteId) {
        Note note = noteMapper.selectById(noteId);
        if (note == null) {
            throw BusinessException.of(ErrorCode.NOT_FOUND, "笔记不存在");
        }
        String key = RedisKey.aiSummary(noteId);
        AiSummaryVO cached = readCache(key);
        if (cached != null) {
            return cached;
        }

        String summary;
        String source;
        if (aiChatClient.isAvailable()) {
            try {
                summary = aiChatClient.complete(SYSTEM_PROMPT, buildPrompt(note));
                source = "ai";
            } catch (Exception e) {
                log.warn("[AI] 总结调用失败, 降级本地摘要: noteId={}, err={}", noteId, e.getMessage());
                summary = localSummary(note);
                source = "local";
            }
        } else {
            summary = localSummary(note);
            source = "local";
        }
        if (summary == null || summary.isBlank()) {
            summary = "这篇笔记暂无足够内容可总结";
        }

        AiSummaryVO vo = new AiSummaryVO();
        vo.setNoteId(noteId);
        vo.setSummary(summary);
        vo.setSource(source);
        writeCache(key, vo);
        return vo;
    }

    @Override
    public void evict(Long noteId) {
        try {
            redisService.delete(RedisKey.aiSummary(noteId));
        } catch (Exception e) {
            log.warn("[AI] 清理总结缓存失败: noteId={}, err={}", noteId, e.getMessage());
        }
    }

    private String buildPrompt(Note note) {
        String content = note.getContent() == null ? "" : note.getContent();
        if (content.length() > MAX_CONTENT_CHARS) {
            content = content.substring(0, MAX_CONTENT_CHARS);
        }
        return "题目标题：" + (note.getQuestionTitle() == null ? "未知" : note.getQuestionTitle())
                + "\n笔记内容：\n" + content;
    }

    private String localSummary(Note note) {
        return MarkdownUtils.excerpt(note.getContent(), LOCAL_SUMMARY_CHARS);
    }

    private AiSummaryVO readCache(String key) {
        try {
            String raw = redisService.get(key);
            return raw == null ? null : objectMapper.readValue(raw, AiSummaryVO.class);
        } catch (Exception e) {
            log.warn("[AI] 读取总结缓存失败(降级生成): {}", e.getMessage());
            return null;
        }
    }

    private void writeCache(String key, AiSummaryVO vo) {
        try {
            redisService.set(key, objectMapper.writeValueAsString(vo), CACHE_TTL);
        } catch (Exception e) {
            log.warn("[AI] 写总结缓存失败(不影响结果): {}", e.getMessage());
        }
    }
}
