package com.notehub.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 异常 → 统一错误码映射(客户端错误不能变 500)
 */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("坏 JSON → 40001")
    void unreadableBody() {
        ApiResponse<Void> resp = handler.handleUnreadable(
                new HttpMessageNotReadableException("bad", (org.springframework.http.HttpInputMessage) null));
        assertEquals(ErrorCode.BAD_PARAM.getCode(), resp.getCode());
    }

    @Test
    @DisplayName("缺参数/类型错 → 40001")
    void missingOrMismatchedParam() throws Exception {
        assertEquals(ErrorCode.BAD_PARAM.getCode(),
                handler.handleMissingParam(new MissingServletRequestParameterException("q", "Long")).getCode());
        assertEquals(ErrorCode.BAD_PARAM.getCode(),
                handler.handleMissingParam(new MethodArgumentTypeMismatchException(
                        "abc", Long.class, "id", null, null)).getCode());
    }

    @Test
    @DisplayName("缺文件字段 → 40001")
    void missingPart() {
        assertEquals(ErrorCode.BAD_PARAM.getCode(),
                handler.handleMissingPart(new MissingServletRequestPartException("file")).getCode());
    }

    @Test
    @DisplayName("超大小限制 → 40001 且文案 5MB")
    void tooLarge() {
        ApiResponse<Void> resp = handler.handleTooLarge(new MaxUploadSizeExceededException(10));
        assertEquals(ErrorCode.BAD_PARAM.getCode(), resp.getCode());
        assertEquals("图片不能超过 5MB", resp.getMessage());
    }
}
