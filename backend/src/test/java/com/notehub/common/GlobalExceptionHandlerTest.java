package com.notehub.common;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.dao.DeadlockLoserDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.Set;

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
    @DisplayName("查询参数绑定失败(page=abc) → 40001 且不透出 Java 类型细节")
    void bindException() {
        BindException e = new BindException(new Object(), "param");
        e.addError(new FieldError("param", "page", "Failed to convert java.lang.String to int"));
        ApiResponse<Void> resp = handler.handleBind(e);
        assertEquals(ErrorCode.BAD_PARAM.getCode(), resp.getCode());
        assertEquals("参数类型不正确", resp.getMessage());
    }

    @Test
    @DisplayName("@Valid 校验: 直接用 DTO 文案, 不重复拼接字段名")
    void validMessageNotDuplicated() {
        MethodArgumentNotValidException e = Mockito.mock(MethodArgumentNotValidException.class);
        org.springframework.validation.BindingResult result =
                new org.springframework.validation.BeanPropertyBindingResult(new Object(), "body");
        result.addError(new FieldError("body", "content", "content 最长 50000 字"));
        Mockito.when(e.getBindingResult()).thenReturn(result);

        assertEquals("content 最长 50000 字", handler.handleValid(e).getMessage());
    }

    @Test
    @DisplayName("类型转换失败(page=abc) → 通用文案, 不泄露 Java 类型")
    void bindingFailureMessageHidden() {
        MethodArgumentNotValidException e = Mockito.mock(MethodArgumentNotValidException.class);
        org.springframework.validation.BindingResult result =
                new org.springframework.validation.BeanPropertyBindingResult(new Object(), "body");
        result.addError(new FieldError("body", "page", "abc", true,
                new String[]{"typeMismatch", "typeMismatch.page"}, null,
                "Failed to convert java.lang.String to int"));
        Mockito.when(e.getBindingResult()).thenReturn(result);

        assertEquals("参数类型不正确", handler.handleValid(e).getMessage());
    }

    @Test
    @DisplayName("方法级约束(非法 email) → 40001")
    void constraintViolation() {
        @SuppressWarnings("unchecked")
        ConstraintViolation<Object> violation = Mockito.mock(ConstraintViolation.class);
        Mockito.when(violation.getMessage()).thenReturn("email 格式不正确");
        ConstraintViolationException e =
                new ConstraintViolationException("bad", Set.of(violation));

        ApiResponse<Void> resp = handler.handleConstraintViolation(e);
        assertEquals(ErrorCode.BAD_PARAM.getCode(), resp.getCode());
        assertEquals("email 格式不正确", resp.getMessage());
    }

    @Test
    @DisplayName("方法不支持 → HTTP 405 + 40500")
    void methodNotAllowed() {
        ResponseEntity<ApiResponse<Void>> resp = handler.handleMethodNotAllowed(
                new HttpRequestMethodNotSupportedException("PUT"));
        assertEquals(HttpStatus.METHOD_NOT_ALLOWED, resp.getStatusCode());
        assertEquals(ErrorCode.METHOD_NOT_ALLOWED.getCode(), resp.getBody().getCode());
    }

    @Test
    @DisplayName("Content-Type 不支持 → HTTP 415 + 41500")
    void mediaTypeNotSupported() {
        ResponseEntity<ApiResponse<Void>> resp = handler.handleMediaType(
                new HttpMediaTypeNotSupportedException("text/plain"));
        assertEquals(HttpStatus.UNSUPPORTED_MEDIA_TYPE, resp.getStatusCode());
        assertEquals(ErrorCode.UNSUPPORTED_MEDIA_TYPE.getCode(), resp.getBody().getCode());
    }

    @Test
    @DisplayName("死锁重试耗尽 → 40902(可重试), 不是 500")
    void deadlockMappedToRetryable() {
        ApiResponse<Void> resp = handler.handleDeadlock(
                new DeadlockLoserDataAccessException("deadlock", new RuntimeException("x")));
        assertEquals(ErrorCode.CONFLICT_RETRY.getCode(), resp.getCode());
    }

    @Test
    @DisplayName("缺文件字段 → 40001; 超大小 → 40001")
    void multipartErrors() {
        assertEquals(ErrorCode.BAD_PARAM.getCode(),
                handler.handleMissingPart(new MissingServletRequestPartException("file")).getCode());
        assertEquals("图片不能超过 5MB",
                handler.handleTooLarge(new MaxUploadSizeExceededException(10)).getMessage());
    }
}
