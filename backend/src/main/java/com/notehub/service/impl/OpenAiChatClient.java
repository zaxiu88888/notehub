package com.notehub.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.notehub.service.AiChatClient;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * OpenAI 兼容客户端: 默认关闭, 配置 ai.summary.enabled=true + api-key 后启用
 * 任何异常由上层捕获并降级为本地摘要
 */
@Log4j2
@Component
public class OpenAiChatClient implements AiChatClient {

    private final boolean available;
    private final String model;
    private final String apiKey;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public OpenAiChatClient(
            @Value("${ai.summary.enabled:false}") boolean enabled,
            @Value("${ai.summary.base-url:https://api.openai.com/v1}") String baseUrl,
            @Value("${ai.summary.model:gpt-4o-mini}") String model,
            @Value("${ai.summary.api-key:}") String apiKey,
            @Value("${ai.summary.timeout-seconds:20}") int timeoutSeconds,
            ObjectMapper objectMapper) {
        this.model = model;
        this.apiKey = apiKey;
        this.objectMapper = objectMapper;
        this.available = enabled && apiKey != null && !apiKey.isBlank();

        String normalizedBase = baseUrl.endsWith("/")
                ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        int timeoutMs = Math.max(timeoutSeconds, 1) * 1000;
        factory.setConnectTimeout(Math.min(timeoutMs, 10_000));
        factory.setReadTimeout(timeoutMs);
        this.restClient = RestClient.builder()
                .baseUrl(normalizedBase)
                .requestFactory(factory)
                .build();
    }

    @Override
    public boolean isAvailable() {
        return available;
    }

    @Override
    public String complete(String systemPrompt, String userPrompt) {
        if (!available) {
            throw new IllegalStateException("AI 总结未启用");
        }
        Map<String, Object> body = Map.of(
                "model", model,
                "temperature", 0.2,
                "max_tokens", 300,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)));

        String response = restClient.post()
                .uri("/chat/completions")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(String.class);

        try {
            JsonNode root = objectMapper.readTree(response);
            String content = root.path("choices").path(0).path("message").path("content").asText("");
            if (content.isBlank()) {
                throw new IllegalStateException("AI 返回内容为空");
            }
            return content.trim();
        } catch (Exception e) {
            log.warn("[AI] 响应解析失败: {}", e.getMessage());
            throw new IllegalStateException("AI 响应解析失败", e);
        }
    }
}
