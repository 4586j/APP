package com.erp.security.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 验证码响应（B1.4 Phase 2）。
 *
 * <p>{@code imageBase64} 已带 {@code data:image/png;base64,} 前缀，前端可直接绑定到
 * &lt;img src="..."&gt;。{@code uuid} 是后端 Redis 里答案的 key 后缀，登录时回传。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CaptchaResponse {

    private String uuid;
    private String imageBase64;
}
