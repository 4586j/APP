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
 * 认证 Controller（B1.4 Phase 1 + Phase 2，B1.5 加 IP 落库）。
 * 对齐 API_DESIGN 第 2 章：login / logout / me / refresh / change-password / captcha。
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
     * 登录（B1.5：把 HttpServletRequest 透给 AuthService 以提取 IP）。
     */
    @PostMapping("/login")
    public R<LoginResponse> login(@Valid @RequestBody LoginRequest req, HttpServletRequest request) {
        log.info("登录请求 username={}", req.getUsername());
        return R.ok(authService.login(req, request));
    }

    @PostMapping("/logout")
    public R<Void> logout(HttpServletRequest request) {
        String bearer = request.getHeader(HttpHeaders.AUTHORIZATION);
        authService.logout(bearer);
        return R.ok();
    }

    @GetMapping("/me")
    public R<UserInfo> me() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null) {
            throw new BusinessException(R.CODE_UNAUTHORIZED, "未登录");
        }
        return R.ok(authService.currentUserInfo(auth.getName()));
    }

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

    @PostMapping("/refresh")
    public R<LoginResponse> refresh(@RequestBody(required = false) RefreshTokenRequest req,
                                    HttpServletRequest request) {
        String token = req == null ? null : req.getRefreshToken();
        if (!StringUtils.hasText(token)) {
            token = request.getHeader(HttpHeaders.AUTHORIZATION);
        }
        return R.ok(authService.refresh(token));
    }

    @GetMapping("/captcha")
    public R<CaptchaResponse> captcha() {
        return R.ok(captchaService.generate());
    }
}
