package com.erp.web.exception;

import com.erp.common.exception.BusinessException;
import com.erp.common.model.R;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * 全局异常处理器（B0.3 骨架）。
 *
 * <p>对齐 API_DESIGN 错误码规范：业务异常返回 200 + R.code，系统异常返回 5xx。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public R<Void> handleBusinessException(BusinessException ex) {
        log.warn("[业务异常] code={} msg={}", ex.getCode(), ex.getMessage());
        return R.fail(ex.getCode(), ex.getMessage());
    }

    @ExceptionHandler(AuthorizationDeniedException.class)
    public ResponseEntity<R<Void>> handleAccessDenied(AuthorizationDeniedException ex) {
        log.warn("[权限不足] {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(R.fail("没有权限，请联系管理员"));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public R<Void> handleValidation(MethodArgumentNotValidException ex) {
        String msg = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return R.fail(R.CODE_PARAM_INVALID, msg);
    }

    @ExceptionHandler(BindException.class)
    public R<Void> handleBind(BindException ex) {
        String msg = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return R.fail(R.CODE_PARAM_INVALID, msg);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<R<Void>> handleAny(Exception ex) {
        log.error("[未捕获异常]", ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(R.fail("系统繁忙，请稍后再试"));
    }
}
