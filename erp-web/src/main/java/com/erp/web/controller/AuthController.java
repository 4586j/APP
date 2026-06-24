package com.erp.web.controller;

import com.erp.common.exception.BusinessException;
import com.erp.common.model.R;
import com.erp.security.dto.LoginRequest;
import com.erp.security.dto.LoginResponse;
import com.erp.security.dto.UserInfo;
import com.erp.security.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证 Controller（B1.4 Phase 1）。
 *
 * <p>对应 API_DESIGN §2.1 / §2.4 / §2.5：login / logout / me。
 * 验证码、RSA、refresh、修改密码留待 Phase 2/3。
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * 登录（Phase 1：明文密码 + BCrypt 校验）。
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
     * Phase 2 接口占位：返回 501。
     */
    @PostMapping("/refresh")
    public R<Void> refresh() {
        throw new BusinessException(501, "refresh 接口将在 B1.4 Phase 2 实现");
    }
}
