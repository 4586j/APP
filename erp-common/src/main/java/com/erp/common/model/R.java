package com.erp.common.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;

/**
 * 统一返回结构（B0.3 骨架）。
 * 对齐 API_DESIGN 规范：{code, message, data, timestamp}
 *
 * @param <T> data 字段类型
 */
@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class R<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    public static final int CODE_SUCCESS = 0;
    public static final int CODE_FAIL = 500;
    public static final int CODE_UNAUTHORIZED = 400 + 1;
    public static final int CODE_PARAM_INVALID = 400;
    public static final int CODE_FORBIDDEN = 400 + 3;
    public static final int CODE_NOT_FOUND = 400 + 4;
    /** WebDAV Locked（423）。 */
    public static final int CODE_LOCKED = 423;

    private int code;
    private String message;
    private T data;
    private Instant timestamp = Instant.now();

    public R(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.timestamp = Instant.now();
    }

    public static <T> R<T> ok() {
        return new R<>(CODE_SUCCESS, "ok", null);
    }

    public static <T> R<T> ok(T data) {
        return new R<>(CODE_SUCCESS, "ok", data);
    }

    public static <T> R<T> ok(T data, String message) {
        return new R<>(CODE_SUCCESS, message, data);
    }

    public static <T> R<T> fail(String message) {
        return new R<>(CODE_FAIL, message, null);
    }

    public static <T> R<T> fail(int code, String message) {
        return new R<>(code, message, null);
    }

    public boolean isSuccess() {
        return this.code == CODE_SUCCESS;
    }
}
