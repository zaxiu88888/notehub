package com.notehub.common;

import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * 全局异常处理: 一切异常都转成统一 ApiResponse, 不向前端暴露堆栈
 */
@Log4j2
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 业务异常: code + message (HTTP 200) */
    @ExceptionHandler(BusinessException.class)
    public ApiResponse<Void> handleBusiness(BusinessException e) {
        log.info("[业务异常] code={}, message={}", e.getErrorCode().getCode(), e.getMessage());
        return ApiResponse.error(e.getErrorCode(), e.getMessage());
    }

    /** @Valid 请求体校验失败: 取第一个字段错误 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResponse<Void> handleValid(MethodArgumentNotValidException e) {
        FieldError fieldError = e.getBindingResult().getFieldError();
        String message = fieldError == null ? ErrorCode.BAD_PARAM.getMessage()
                : fieldError.getField() + " " + fieldError.getDefaultMessage();
        return ApiResponse.error(ErrorCode.BAD_PARAM, message);
    }

    /** 方法级参数校验(@RequestParam 上的约束) */
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ApiResponse<Void> handleMethodValidation(HandlerMethodValidationException e) {
        return ApiResponse.error(ErrorCode.BAD_PARAM, e.getMessage());
    }

    /** 静态资源/路径不存在 → 40401 */
    @ExceptionHandler(NoResourceFoundException.class)
    public ApiResponse<Void> handleNoResource(NoResourceFoundException e) {
        return ApiResponse.error(ErrorCode.NOT_FOUND, "接口不存在: " + e.getResourcePath());
    }

    /** 请求体不是合法 JSON → 40001(而不是 500) */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ApiResponse<Void> handleUnreadable(HttpMessageNotReadableException e) {
        log.info("[参数异常] 请求体解析失败: {}", e.getMessage());
        return ApiResponse.error(ErrorCode.BAD_PARAM, "请求体格式错误");
    }

    /** 缺少必填 query 参数 / 类型不匹配 → 40001 */
    @ExceptionHandler({MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class})
    public ApiResponse<Void> handleMissingParam(Exception e) {
        return ApiResponse.error(ErrorCode.BAD_PARAM, "参数缺失或类型不正确");
    }

    /** 缺少 multipart 文件字段 → 40001 */
    @ExceptionHandler(MissingServletRequestPartException.class)
    public ApiResponse<Void> handleMissingPart(MissingServletRequestPartException e) {
        return ApiResponse.error(ErrorCode.BAD_PARAM, "缺少文件字段: " + e.getRequestPartName());
    }

    /** 超过容器大小限制(5MB) → 40001, 不让它变成 500 */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ApiResponse<Void> handleTooLarge(MaxUploadSizeExceededException e) {
        return ApiResponse.error(ErrorCode.BAD_PARAM, "图片不能超过 5MB");
    }

    /** 兜底: 系统异常 (HTTP 500) */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception e) {
        log.error("[系统异常] {}", e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(ErrorCode.INTERNAL_ERROR));
    }
}
