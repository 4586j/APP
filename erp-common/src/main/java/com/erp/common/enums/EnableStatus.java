package com.erp.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 通用启用状态枚举（B0.3 骨架）。
 */
@Getter
@AllArgsConstructor
public enum EnableStatus {

    DISABLED(0, "禁用"),
    ENABLED(1, "启用");

    private final Integer code;
    private final String desc;
}
