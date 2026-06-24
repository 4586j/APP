package com.erp.web.controller;

import com.erp.common.exception.BusinessException;
import com.erp.common.model.R;
import com.erp.security.dto.CaptchaResponse;
import com.erp.security.dto.ChangePasswordRequest;
import com.erp.security.dto.LoginRequest;
import com.erp.security.dto.LoginResponse;
import com.erp.security.dto.RefreshTokenRequest;
import com.erp.security.dto.UserInfo;
import com.erp.security.service.AuthService;
import com.erp.security.service.CaptchaService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证 Controller（B1.4 Phase 1 + Phase 2）。
 *
 * <p>对齐 API_DESIGN §2.x：login / logout / me / refresh / change-password / captcha。
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final CaptchaService captchaService;

    public AuthController(AuthService authService, CaptchaService captchaService) {
        this.authService = authService;
        this.captchaService = captchaService;
    }

    /**
     * 登录（Phase 1：明文密码 + BCrypt；Phase 2：可选 captcha）。
     */
    @PostMapping("/login")
    public R<LoginResponse> login(@Valid @RequestBody LoginRequest req) {
        log.info("登录请求 username={}", req.getUsername());
        return R.ok(authService.login(req));
    }

    /**
     * 登出：把当前 access token 的 jti 拉黑。
     */
    @PostMapping("/logout")
    public R<Void> logout(HttpServletRequest request) {
        String bearer = request.getHeader(HttpHeaders.AUTHORIZATION);
        authService.logout(bearer);
        return R.ok();
    }

    /**
     * 当前用户信息（每次都从 UserDetailsLoader 重新加载，保证权限最新）。
     */
    @GetMapping("/me")
    public R<UserInfo> me() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null) {
            throw new BusinessException(R.CODE_UNAUTHORIZED, "未登录");
        }
        return R.ok(authService.currentUserInfo(auth.getName()));
    }

    /**
     * 修改密码（B1.4 Phase 2）。需登录态。
     * 成功后当前 access token 立即拉黑，前端应跳登录页。
     */
    @PostMapping("/change-password")
    public R<Void> changePassword(@Valid @RequestBody ChangePasswordRequest req,
                                  HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null) {
            throw new BusinessException(R.CODE_UNAUTHORIZED, "未登录");
        }
        String bearer = request.getHeader(HttpHeaders.AUTHORIZATION);
        authService.changePassword(auth.getName(), req, bearer);
        return R.ok();
    }

    /**
     * 刷新 token（B1.4 Phase 2）。白名单接口，不要求登录态。
     *
     * <p>refresh token 来源：优先 body 里的 {@code refreshToken} 字段；
     * 缺失时 fallback 到 {@code Authorization: Bearer <refreshToken>} 头。
     * 成功后旧 refresh 立即拉黑（一次性、防重放）。
     */
    @PostMapping("/refresh")
    public R<LoginResponse> refresh(@RequestBody(required = false) RefreshTokenRequest req,
                                    HttpServletRequest request) {
        String token = req == null ? null : req.getRefreshToken();
        if (!StringUtils.hasText(token)) {
            // fallback：Authorization 头
            token = request.getHeader(HttpHeaders.AUTHORIZATION);
        }
        return R.ok(authService.refresh(token));
    }

    /**
     * 颁发验证码（B1.4 Phase 2）。白名单接口。
     * 返回 uuid + PNG（data URL），答案在 Redis 中 TTL 5 分钟。
     */
    @GetMapping("/captcha")
    public R<CaptchaResponse> captcha() {
        return R.ok(captchaService.generate());
    }
}
