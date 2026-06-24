package com.erp.security.dto;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登录请求 DTO（B1.4 Phase 1 / Phase 2）。
 *
 * <p>Phase 1：username + password 明文（HTTPS 兜底）。
 * <p>Phase 2：追加可选字段 {@code captchaUuid + captchaCode}；当
 * {@code app.security.captcha.enabled=true} 时强制校验，否则忽略。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {

    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;

    /** 验证码 uuid（{@link com.erp.security.service.CaptchaService} 颁发）。 */
    @Nullable
    private String captchaUuid;

    /** 验证码答案（不区分大小写）。 */
    @Nullable
    private String captchaCode;

    /** 兼容仅有 username/password 两参数的旧调用。 */
    public LoginRequest(String username, String password) {
        this.username = username;
        this.password = password;
    }
}
