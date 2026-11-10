package com.notehub.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.notehub.common.BusinessException;
import com.notehub.common.ErrorCode;
import com.notehub.mapper.NoteMapper;
import com.notehub.model.entity.Note;
import com.notehub.model.vo.note.AiSummaryVO;
import com.notehub.service.impl.AiSummaryServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiSummaryServiceImplTest {

    private NoteMapper noteMapper;
    private RedisService redisService;
    private AiChatClient aiChatClient;
    private RateLimitService rateLimitService;
    private ObjectMapper objectMapper;
    private AiSummaryServiceImpl service;

    @BeforeEach
    void setUp() {
        noteMapper = mock(NoteMapper.class);
        redisService = mock(RedisService.class);
        aiChatClient = mock(AiChatClient.class);
        rateLimitService = mock(RateLimitService.class);
        objectMapper = new ObjectMapper();
        service = new AiSummaryServiceImpl(noteMapper, redisService, aiChatClient,
                rateLimitService, objectMapper);
    }

    private AiSummaryVO callSummarize() {
        return service.summarize(1L, 2L);
    }

    private Note note() {
        Note note = new Note();
        note.setNoteId(1L);
        note.setQuestionTitle("两数之和");
        note.setContent("# 两数之和题解\n用哈希表一次遍历，时间复杂度 O(n)。");
        return note;
    }

    @Test
    @DisplayName("未配置 AI: 本地摘要, 写缓存")
    void localFallbackWhenAiUnavailable() {
        when(noteMapper.selectById(1L)).thenReturn(note());
        when(aiChatClient.isAvailable()).thenReturn(false);
        when(redisService.get("ai:summary:1")).thenReturn(null);

        AiSummaryVO vo = callSummarize();

        assertEquals("local", vo.getSource());
        assertTrue(vo.getSummary().contains("哈希表"));
        verify(aiChatClient, never()).complete(anyString(), anyString());
        verify(rateLimitService, never()).tryAcquire(anyString(), anyInt(), any(Duration.class));
        verify(redisService).set(eq("ai:summary:1"), anyString(), any(Duration.class));
    }

    @Test
    @DisplayName("缓存命中: 不再调用模型")
    void cacheHitSkipsAi() throws Exception {
        AiSummaryVO cached = new AiSummaryVO();
        cached.setNoteId(1L);
        cached.setSummary("缓存摘要");
        cached.setSource("ai");
        when(noteMapper.selectById(1L)).thenReturn(note());
        when(redisService.get("ai:summary:1"))
                .thenReturn(objectMapper.writeValueAsString(cached));

        AiSummaryVO vo = callSummarize();

        assertEquals("缓存摘要", vo.getSummary());
        verify(aiChatClient, never()).isAvailable();
        verify(aiChatClient, never()).complete(anyString(), anyString());
        verify(rateLimitService, never()).tryAcquire(anyString(), anyInt(), any(Duration.class));
    }

    @Test
    @DisplayName("配置 AI: 返回模型摘要")
    void aiSummaryWhenAvailable() {
        when(noteMapper.selectById(1L)).thenReturn(note());
        when(redisService.get("ai:summary:1")).thenReturn(null);
        when(aiChatClient.isAvailable()).thenReturn(true);
        when(rateLimitService.tryAcquire(eq("rate:ai:summary:u2"), eq(5), any(Duration.class)))
                .thenReturn(true);
        when(aiChatClient.complete(anyString(), anyString())).thenReturn("1. 哈希表\n2. O(n)");

        AiSummaryVO vo = callSummarize();

        assertEquals("ai", vo.getSource());
        assertEquals("1. 哈希表\n2. O(n)", vo.getSummary());
        verify(rateLimitService).tryAcquire(eq("rate:ai:summary:u2"), eq(5), any(Duration.class));
    }

    @Test
    @DisplayName("模型调用失败: 降级本地摘要")
    void aiFailureFallsBackLocal() {
        when(noteMapper.selectById(1L)).thenReturn(note());
        when(redisService.get("ai:summary:1")).thenReturn(null);
        when(aiChatClient.isAvailable()).thenReturn(true);
        when(rateLimitService.tryAcquire(anyString(), anyInt(), any(Duration.class)))
                .thenReturn(true);
        when(aiChatClient.complete(anyString(), anyString()))
                .thenThrow(new IllegalStateException("timeout"));

        AiSummaryVO vo = callSummarize();

        assertEquals("local", vo.getSource());
        assertTrue(vo.getSummary().contains("哈希表"));
    }

    @Test
    @DisplayName("模型配额用尽: 42901, 不调用模型")
    void aiRateLimited() {
        when(noteMapper.selectById(1L)).thenReturn(note());
        when(redisService.get("ai:summary:1")).thenReturn(null);
        when(aiChatClient.isAvailable()).thenReturn(true);
        when(rateLimitService.tryAcquire(anyString(), anyInt(), any(Duration.class)))
                .thenReturn(false);

        BusinessException e = assertThrows(BusinessException.class, this::callSummarize);

        assertEquals(ErrorCode.TOO_MANY_REQUESTS, e.getErrorCode());
        verify(aiChatClient, never()).complete(anyString(), anyString());
    }

    @Test
    @DisplayName("本地缓存但模型已可用: 重新生成升级")
    void localCacheUpgradesWhenAiAvailable() throws Exception {
        AiSummaryVO cached = new AiSummaryVO();
        cached.setNoteId(1L);
        cached.setSummary("旧本地摘要");
        cached.setSource("local");
        when(noteMapper.selectById(1L)).thenReturn(note());
        when(redisService.get("ai:summary:1"))
                .thenReturn(objectMapper.writeValueAsString(cached));
        when(aiChatClient.isAvailable()).thenReturn(true);
        when(rateLimitService.tryAcquire(anyString(), anyInt(), any(Duration.class)))
                .thenReturn(true);
        when(aiChatClient.complete(anyString(), anyString())).thenReturn("新 AI 摘要");

        AiSummaryVO vo = callSummarize();

        assertEquals("ai", vo.getSource());
        assertEquals("新 AI 摘要", vo.getSummary());
    }

    @Test
    @DisplayName("笔记不存在 → 40401; evict 删缓存")
    void notFoundAndEvict() {
        when(noteMapper.selectById(404L)).thenReturn(null);
        BusinessException e = assertThrows(BusinessException.class,
                () -> service.summarize(404L, 2L));
        assertEquals(ErrorCode.NOT_FOUND, e.getErrorCode());

        service.evict(1L);
        verify(redisService).delete("ai:summary:1");
    }
}
