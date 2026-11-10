package com.notehub.service;

/**
 * AI 对话客户端(当前实现 OpenAI 兼容 /chat/completions)
 */
public interface AiChatClient {

    /** 是否已配置可用(开关打开且 API Key 非空) */
    boolean isAvailable();

    String complete(String systemPrompt, String userPrompt);
}
