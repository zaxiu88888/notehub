package com.notehub.common;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.log4j.Log4j2;
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
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * 全局异常处理: 一切异常都转成统一 ApiResponse, 不向前端暴露内部细节
 * 约定: 业务/参数错误 HTTP 200 + 业务码; 纯 HTTP 语义错误(405/415) 用对应状态码
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

    /**
     * @Valid 请求体校验失败: 直接取 DTO 的 message
     * (本项目 DTO 文案已自带字段名, 不再拼接, 避免 "content content 最长..." 重复)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResponse<Void> handleValid(MethodArgumentNotValidException e) {
        FieldError fieldError = e.getBindingResult().getFieldError();
        String message;
        if (fieldError == null || fieldError.getDefaultMessage() == null) {
            message = ErrorCode.BAD_PARAM.getMessage();
        } else if (fieldError.isBindingFailure()) {
            // 类型转换失败(page=abc)会带 Java 类型细节, 统一为通用文案
            message = "参数类型不正确";
        } else {
            message = fieldError.getDefaultMessage();
        }
        return ApiResponse.error(ErrorCode.BAD_PARAM, message);
    }

    /** query 参数绑定到对象失败(如 page=abc): 统一文案, 不透出 Java 类型细节 */
    @ExceptionHandler(BindException.class)
    public ApiResponse<Void> handleBind(BindException e) {
        log.info("[参数异常] 参数绑定失败: {}", e.getMessage());
        return ApiResponse.error(ErrorCode.BAD_PARAM, "参数类型不正确");
    }

    /** 方法级参数校验(@RequestParam 上的 @Email/@NotBlank 等) */
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ApiResponse<Void> handleMethodValidation(HandlerMethodValidationException e) {
        log.info("[参数异常] 方法参数校验失败: {}", e.getMessage());
        return ApiResponse.error(ErrorCode.BAD_PARAM, "参数校验失败");
    }

    /** 方法参数约束(类级 @Validated 触发) */
    @ExceptionHandler(ConstraintViolationException.class)
    public ApiResponse<Void> handleConstraintViolation(ConstraintViolationException e) {
        String message = e.getConstraintViolations().stream()
                .findFirst()
                .map(v -> v.getMessage())
                .orElse(ErrorCode.BAD_PARAM.getMessage());
        return ApiResponse.error(ErrorCode.BAD_PARAM, message);
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

    /** 方法不支持: 按 HTTP 语义返回 405 */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodNotAllowed(HttpRequestMethodNotSupportedException e) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(ApiResponse.error(ErrorCode.METHOD_NOT_ALLOWED,
                        "该接口不支持 " + e.getMethod() + " 方法"));
    }

    /** Content-Type 不支持: 按 HTTP 语义返回 415 */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMediaType(HttpMediaTypeNotSupportedException e) {
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .body(ApiResponse.error(ErrorCode.UNSUPPORTED_MEDIA_TYPE));
    }

    /** 数据库死锁(重试后仍失败): 可安全重试的业务冲突, 不暴露为 500 */
    @ExceptionHandler(DeadlockLoserDataAccessException.class)
    public ApiResponse<Void> handleDeadlock(DeadlockLoserDataAccessException e) {
        log.warn("[并发冲突] 数据库死锁: {}", e.getMessage());
        return ApiResponse.error(ErrorCode.CONFLICT_RETRY);
    }

    /** 兜底: 系统异常 (HTTP 500) */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception e) {
        log.error("[系统异常] {}", e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(ErrorCode.INTERNAL_ERROR));
    }
}
