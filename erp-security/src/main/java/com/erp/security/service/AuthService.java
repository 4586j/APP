package com.erp.security.service;

import com.erp.common.exception.BusinessException;
import com.erp.common.model.R;
import com.erp.security.captcha.CaptchaProperties;
import com.erp.security.dto.ChangePasswordRequest;
import com.erp.security.dto.LoginRequest;
import com.erp.security.dto.LoginResponse;
import com.erp.security.dto.UserInfo;
import com.erp.security.token.JwtTokenProvider;
import com.erp.security.token.TokenBlacklist;
import com.erp.security.token.TokenType;
import com.erp.security.user.LoginUser;
import com.erp.security.user.UserDetailsLoader;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Objects;

/**
 * 认证业务服务（B1.4 Phase 1，B1.5 增强）。
 *
 * <p>承担登录密码校验、签发 token、登出拉黑、查询当前用户、改密、refresh。
 * B1.5 接入失败锁定 + 登录时间/IP 落库（通过 UserDetailsLoader 回调）。
 */
@Slf4j
@Service
public class AuthService {

    private final UserDetailsLoader userDetailsLoader;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final TokenBlacklist tokenBlacklist;
    private final CaptchaService captchaService;
    private final CaptchaProperties captchaProperties;

    public AuthService(UserDetailsLoader userDetailsLoader,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider jwtTokenProvider,
                       TokenBlacklist tokenBlacklist,
                       CaptchaService captchaService,
                       CaptchaProperties captchaProperties) {
        this.userDetailsLoader = userDetailsLoader;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.tokenBlacklist = tokenBlacklist;
        this.captchaService = captchaService;
        this.captchaProperties = captchaProperties;
    }

    /**
     * 登录：校验用户名 + BCrypt 密码，签发 access + refresh token。
     * 当 {@code app.security.captcha.enabled=true} 时，先校验验证码（错则 401）。
     * B1.5：登录失败累计计数（达阈值锁定 15 分钟）；登录成功清零计数 + 写 lastLoginAt/Ip。
     *
     * @param req     登录请求
     * @param request 用于取 client IP（可为 null，单测时传 null）
     * @throws BusinessException 用户不存在 / 密码错 / 验证码错（统一 401，避免账号枚举）
     */
    public LoginResponse login(LoginRequest req, HttpServletRequest request) {
        if (captchaProperties.isEnabled()) {
            if (!captchaService.verify(req.getCaptchaUuid(), req.getCaptchaCode())) {
                log.info("登录失败：验证码错误 username={}", req.getUsername());
                throw new BusinessException(R.CODE_UNAUTHORIZED, "验证码错误");
            }
        }
        LoginUser user;
        try {
            user = userDetailsLoader.loadByUsername(req.getUsername());
        } catch (UsernameNotFoundException ex) {
            log.info("登录失败：用户不存在/已禁用/已锁定 username={}", req.getUsername());
            throw new BusinessException(R.CODE_UNAUTHORIZED, "用户名或密码错误");
        }
        if (user == null || !passwordEncoder.matches(req.getPassword(), user.getEncryptedPassword())) {
            int failures = userDetailsLoader.onLoginFailure(req.getUsername());
            log.info("登录失败：密码错误 username={}, failures={}", req.getUsername(), failures);
            throw new BusinessException(R.CODE_UNAUTHORIZED, "用户名或密码错误");
        }
        // 登录成功
        String ip = extractClientIp(request);
        userDetailsLoader.onLoginSuccess(user.getUsername(), ip);

        String access = jwtTokenProvider.generateAccessToken(user.getId(), user.getUsername(), user.getRoles(), user.getPermissions());
        String refresh = jwtTokenProvider.generateRefreshToken(user.getId(), user.getUsername(), user.getRoles());
        return LoginResponse.builder()
                .accessToken(access)
                .refreshToken(refresh)
                .userInfo(toUserInfo(user))
                .build();
    }

    /** 兼容老调用：不带 HttpServletRequest。 */
    public LoginResponse login(LoginRequest req) {
        return login(req, null);
    }

    /**
     * 登出：解析 token 取出 jti，写 Redis 黑名单。
     */
    public void logout(String bearerToken) {
        String token = stripBearer(bearerToken);
        if (!StringUtils.hasText(token)) {
            return;
        }
        try {
            Claims claims = jwtTokenProvider.parseToken(token);
            long ttl = jwtTokenProvider.getRemainingMillis(token);
            tokenBlacklist.revoke(claims.getId(), ttl);
        } catch (JwtException ex) {
            log.debug("登出时 token 已不可解析，忽略: {}", ex.getMessage());
        }
    }

    /** 获取当前登录用户的 UserInfo。 */
    public UserInfo currentUserInfo(String username) {
        if (!StringUtils.hasText(username)) {
            throw new BusinessException(R.CODE_UNAUTHORIZED, "未登录");
        }
        LoginUser user;
        try {
            user = userDetailsLoader.loadByUsername(username);
        } catch (UsernameNotFoundException ex) {
            throw new BusinessException(R.CODE_UNAUTHORIZED, "用户不存在或已被禁用");
        }
        return toUserInfo(user);
    }

    public void changePassword(String username, ChangePasswordRequest req, String currentBearer) {
        if (!StringUtils.hasText(username)) {
            throw new BusinessException(R.CODE_UNAUTHORIZED, "未登录");
        }
        if (!Objects.equals(req.getNewPassword(), req.getConfirmPassword())) {
            throw new BusinessException(R.CODE_PARAM_INVALID, "两次输入的新密码不一致");
        }
        if (req.getNewPassword() == null || req.getNewPassword().length() < 8) {
            throw new BusinessException(R.CODE_PARAM_INVALID, "新密码至少 8 位");
        }
        if (Objects.equals(req.getOldPassword(), req.getNewPassword())) {
            throw new BusinessException(R.CODE_PARAM_INVALID, "新密码不能与旧密码相同");
        }
        LoginUser user;
        try {
            user = userDetailsLoader.loadByUsername(username);
        } catch (UsernameNotFoundException ex) {
            throw new BusinessException(R.CODE_UNAUTHORIZED, "用户不存在或已被禁用");
        }
        if (!passwordEncoder.matches(req.getOldPassword(), user.getEncryptedPassword())) {
            log.info("修改密码失败：旧密码不匹配 username={}", username);
            throw new BusinessException(R.CODE_PARAM_INVALID, "旧密码不正确");
        }
        String newEncrypted = passwordEncoder.encode(req.getNewPassword());
        userDetailsLoader.updatePassword(username, newEncrypted);
        log.info("用户 {} 修改密码成功", username);
        revokeBearer(currentBearer);
    }

    public LoginResponse refresh(String refreshTokenRaw) {
        String token = stripBearer(refreshTokenRaw);
        if (!StringUtils.hasText(token)) {
            throw new BusinessException(R.CODE_UNAUTHORIZED, "refresh token 缺失");
        }
        Claims claims;
        try {
            claims = jwtTokenProvider.parseToken(token);
        } catch (JwtException ex) {
            log.info("refresh 失败：token 解析错误 {}", ex.getMessage());
            throw new BusinessException(R.CODE_UNAUTHORIZED, "refresh token 无效或已过期");
        }
        String typeStr = claims.get(JwtTokenProvider.CLAIM_TYPE, String.class);
        if (typeStr == null || TokenType.valueOf(typeStr) != TokenType.REFRESH) {
            throw new BusinessException(R.CODE_UNAUTHORIZED, "token 类型不正确，必须使用 refresh token");
        }
        if (tokenBlacklist.isRevoked(claims.getId())) {
            throw new BusinessException(R.CODE_UNAUTHORIZED, "refresh token 已被撤销");
        }
        Number userIdNum = (Number) claims.get(JwtTokenProvider.CLAIM_USER_ID);
        Long userId = userIdNum == null ? null : userIdNum.longValue();
        String username = claims.get(JwtTokenProvider.CLAIM_USERNAME, String.class);
        @SuppressWarnings("unchecked")
        java.util.List<String> roles =
                (java.util.List<String>) claims.getOrDefault(JwtTokenProvider.CLAIM_ROLES, java.util.List.of());

        long ttl = jwtTokenProvider.getRemainingMillis(token);
        tokenBlacklist.revoke(claims.getId(), ttl);

        LoginUser refreshedUser = userDetailsLoader.loadByUsername(username);
        java.util.List<String> refreshedRoles = refreshedUser.getRoles() == null ? roles : refreshedUser.getRoles();
        java.util.List<String> refreshedPermissions = refreshedUser.getPermissions() == null ? java.util.List.of() : refreshedUser.getPermissions();
        String newAccess = jwtTokenProvider.generateAccessToken(userId, username, refreshedRoles, refreshedPermissions);
        String newRefresh = jwtTokenProvider.generateRefreshToken(userId, username, refreshedRoles);

        UserInfo info = toUserInfo(refreshedUser);
        return LoginResponse.builder()
                .accessToken(newAccess)
                .refreshToken(newRefresh)
                .userInfo(info)
                .build();
    }

    private void revokeBearer(String bearer) {
        String token = stripBearer(bearer);
        if (!StringUtils.hasText(token)) {
            return;
        }
        try {
            Claims c = jwtTokenProvider.parseToken(token);
            long ttl = jwtTokenProvider.getRemainingMillis(token);
            tokenBlacklist.revoke(c.getId(), ttl);
        } catch (JwtException ex) {
            log.debug("拉黑时 token 不可解析: {}", ex.getMessage());
        }
    }

    private UserInfo toUserInfo(LoginUser u) {
        return UserInfo.builder()
                .id(u.getId())
                .username(u.getUsername())
                .realName(u.getRealName())
                .department(u.getDepartment())
                .departmentName(u.getDepartmentName())
                .departmentId(u.getDepartmentId())
                .roles(u.getRoles())
                .permissions(u.getPermissions())
                .build();
    }

    /** 取客户端 IP：优先 X-Forwarded-For 首段，否则 X-Real-IP，否则 remoteAddr。 */
    static String extractClientIp(HttpServletRequest request) {
        if (request == null) return "unknown";
        String xff = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(xff)) {
            int comma = xff.indexOf(',');
            String first = (comma > 0 ? xff.substring(0, comma) : xff).trim();
            if (StringUtils.hasText(first)) return first;
        }
        String xri = request.getHeader("X-Real-IP");
        if (StringUtils.hasText(xri)) return xri.trim();
        String remote = request.getRemoteAddr();
        return StringUtils.hasText(remote) ? remote : "unknown";
    }

    private static String stripBearer(String header) {
        if (header == null) return null;
        return header.startsWith("Bearer ") ? header.substring(7).trim() : header.trim();
    }
}
