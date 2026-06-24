package com.erp.security.captcha;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 验证码开关配置（B1.4 Phase 2）。
 *
 * <p>{@code app.security.captcha.enabled}：true 时登录强制校验验证码；
 * dev profile 默认 false 以便集成测试和本地联调。
 */
@ConfigurationProperties(prefix = "app.security.captcha")
public class CaptchaProperties {

    private boolean enabled = false;
    private int ttlMinutes = 5;
    private int length = 4;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getTtlMinutes() {
        return ttlMinutes;
    }

    public void setTtlMinutes(int ttlMinutes) {
        this.ttlMinutes = ttlMinutes;
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }
}
