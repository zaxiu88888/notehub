package com.notehub;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 测试夹具: 共享的纯 ObjectMapper
 */
public final class TestObjectMapper {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private TestObjectMapper() {
    }

    public static ObjectMapper get() {
        return MAPPER;
    }
}
