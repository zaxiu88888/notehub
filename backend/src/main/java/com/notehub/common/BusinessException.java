package com.notehub.common;

import lombok.Getter;

/**
 * 业务异常: 携带 ErrorCode, 由 GlobalExceptionHandler 统一转 ApiResponse
 */
@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    /** 快捷抛出: throw BusinessException.of(ErrorCode.NOT_FOUND) */
    public static BusinessException of(ErrorCode errorCode) {
        return new BusinessException(errorCode);
    }

    public static BusinessException of(ErrorCode errorCode, String message) {
        return new BusinessException(errorCode, message);
    }
}
