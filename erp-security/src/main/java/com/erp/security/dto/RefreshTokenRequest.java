package com.erp.security.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 刷新 token 请求 DTO（B1.4 Phase 2）。
 *
 * <p>实现说明：本接口接受 body 里的 {@code refreshToken} 字段；
 * 也允许通过 Header {@code Authorization: Bearer <refreshToken>} 传入（二选一）。
 * Controller 会优先看 body，再看 Authorization 头。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RefreshTokenRequest {

    private String refreshToken;
}
