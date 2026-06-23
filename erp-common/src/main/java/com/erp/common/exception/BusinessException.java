package com.erp.common.exception;

import lombok.Getter;

/**
 * 业务异常基类（B0.3 骨架）。
 * <p>由 GlobalExceptionHandler 捕获并转 R.fail(code, message)。
 */
@Getter
public class BusinessException extends RuntimeException {

    private final int code;

    public BusinessException(String message) {
        super(message);
        this.code = 500;
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public BusinessException(int code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }
}
