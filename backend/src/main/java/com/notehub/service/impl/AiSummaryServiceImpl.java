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
import com.notehub.service.RateLimitService;
import com.notehub.service.RedisService;
import com.notehub.utils.LocalSummarizer;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

@Log4j2
@Service
public class AiSummaryServiceImpl implements AiSummaryService {

    /** AI 结果缓存 6h; 本地摘要 10min(配置模型后能较快升级) */
    private static final Duration AI_CACHE_TTL = Duration.ofHours(6);
    private static final Duration LOCAL_CACHE_TTL = Duration.ofMinutes(10);
    private static final int MAX_CONTENT_CHARS = 6000;
    private static final int LOCAL_SUMMARY_CHARS = 200;
    private static final int AI_SUMMARY_CHARS = 1000;
    private static final int AI_LIMIT_PER_MINUTE = 5;
    private static final String SYSTEM_PROMPT = """
            你是刷题社区的题解助手。请用简体中文总结用户题解的解题思路，
            输出 3 条以内的要点，总字数不超过 200 字；只基于给定内容，不要编造。
            """;

    private final NoteMapper noteMapper;
    private final RedisService redisService;
    private final AiChatClient aiChatClient;
    private final RateLimitService rateLimitService;
    private final ObjectMapper objectMapper;
    /** 同一笔记并发请求只生成一次(缓存未命中时) */
    private final ConcurrentHashMap<Long, Object> generationLocks = new ConcurrentHashMap<>();

    public AiSummaryServiceImpl(NoteMapper noteMapper, RedisService redisService,
                                AiChatClient aiChatClient, RateLimitService rateLimitService,
                                ObjectMapper objectMapper) {
        this.noteMapper = noteMapper;
        this.redisService = redisService;
        this.aiChatClient = aiChatClient;
        this.rateLimitService = rateLimitService;
        this.objectMapper = objectMapper;
    }

    @Override
    public AiSummaryVO summarize(Long noteId, Long userId) {
        Note note = noteMapper.selectById(noteId);
        if (note == null) {
            throw BusinessException.of(ErrorCode.NOT_FOUND, "笔记不存在");
        }
        String key = RedisKey.aiSummary(noteId);
        AiSummaryVO cached = readCache(key);
        if (cached != null && !shouldUpgrade(cached)) {
            return cached;
        }

        Object lock = generationLocks.computeIfAbsent(noteId, id -> new Object());
        try {
            synchronized (lock) {
                // 双重检查: 等锁期间可能已被其他请求生成
                cached = readCache(key);
                if (cached != null && !shouldUpgrade(cached)) {
                    return cached;
                }
                return generate(note, key, userId);
            }
        } finally {
            generationLocks.remove(noteId, lock);
        }
    }

    @Override
    public void evict(Long noteId) {
        try {
            redisService.delete(RedisKey.aiSummary(noteId));
        } catch (Exception e) {
            log.warn("[AI] 清理总结缓存失败: noteId={}, err={}", noteId, e.getMessage());
        }
    }

    /** 本地缓存但模型已可用 → 重新生成升级 */
    private boolean shouldUpgrade(AiSummaryVO cached) {
        return "local".equals(cached.getSource()) && aiChatClient.isAvailable();
    }

    private AiSummaryVO generate(Note note, String key, Long userId) {
        String summary;
        String source;
        Duration ttl;

        if (aiChatClient.isAvailable()) {
            // 只有真正调用模型才消耗限流配额; 缓存命中/本地摘要不限
            if (!rateLimitService.tryAcquire("rate:ai:summary:u" + userId,
                    AI_LIMIT_PER_MINUTE, Duration.ofMinutes(1))) {
                throw BusinessException.of(ErrorCode.TOO_MANY_REQUESTS);
            }
            try {
                summary = truncate(aiChatClient.complete(SYSTEM_PROMPT, buildPrompt(note)),
                        AI_SUMMARY_CHARS);
                source = "ai";
                ttl = AI_CACHE_TTL;
            } catch (Exception e) {
                log.warn("[AI] 总结调用失败, 降级本地摘要: noteId={}, err={}",
                        note.getNoteId(), e.getMessage());
                summary = localSummary(note);
                source = "local";
                ttl = LOCAL_CACHE_TTL;
            }
        } else {
            summary = localSummary(note);
            source = "local";
            ttl = LOCAL_CACHE_TTL;
        }
        if (summary == null || summary.isBlank()) {
            summary = "这篇笔记暂无足够内容可总结";
        }

        AiSummaryVO vo = new AiSummaryVO();
        vo.setNoteId(note.getNoteId());
        vo.setSummary(summary);
        vo.setSource(source);
        writeCache(key, vo, ttl);
        return vo;
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
        return LocalSummarizer.summarize(note.getContent(), LOCAL_SUMMARY_CHARS);
    }

    private String truncate(String text, int maxChars) {
        if (text == null) {
            return "";
        }
        String trimmed = text.trim();
        return trimmed.length() <= maxChars ? trimmed : trimmed.substring(0, maxChars) + "…";
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

    private void writeCache(String key, AiSummaryVO vo, Duration ttl) {
        try {
            redisService.set(key, objectMapper.writeValueAsString(vo), ttl);
        } catch (Exception e) {
            log.warn("[AI] 写总结缓存失败(不影响结果): {}", e.getMessage());
        }
    }
}
