package com.erp.security.token;

/**
 * JWT token 类型枚举（B1.4 Phase 1）。
 *
 * <p>放入 claim {@code type}，由过滤器 / refresh 接口区分访问令牌与刷新令牌。
 */
public enum TokenType {

    /** 访问令牌（短期，30 分钟）。 */
    ACCESS,

    /** 刷新令牌（长期，30 天，仅用于换 access）。 */
    REFRESH
}
