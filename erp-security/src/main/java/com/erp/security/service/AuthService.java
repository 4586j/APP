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
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Objects;

/**
 * 认证业务服务（B1.4 Phase 1）。
 *
 * <p>承担登录密码校验、签发 token、登出拉黑、查询当前用户。
 * 不在 Controller 写逻辑是为了 Phase 2/3（验证码、防暴力、refresh）扩展时只动这里。
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
     *
     * @throws BusinessException 用户不存在 / 密码错 / 验证码错误（统一 401，避免账号枚举）
     */
    public LoginResponse login(LoginRequest req) {
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
            log.info("登录失败：用户不存在 username={}", req.getUsername());
            throw new BusinessException(R.CODE_UNAUTHORIZED, "用户名或密码错误");
        }
        if (user == null || !passwordEncoder.matches(req.getPassword(), user.getEncryptedPassword())) {
            log.info("登录失败：密码错误 username={}", req.getUsername());
            throw new BusinessException(R.CODE_UNAUTHORIZED, "用户名或密码错误");
        }
        String access = jwtTokenProvider.generateAccessToken(user.getId(), user.getUsername(), user.getRoles());
        String refresh = jwtTokenProvider.generateRefreshToken(user.getId(), user.getUsername(), user.getRoles());
        return LoginResponse.builder()
                .accessToken(access)
                .refreshToken(refresh)
                .userInfo(toUserInfo(user))
                .build();
    }

    /**
     * 登出：解析 token 取出 jti，写 Redis 黑名单。
     * token 为空或解析失败时视为无操作，幂等返回。
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

    /**
     * 获取当前登录用户的 UserInfo（按登录名重新加载，保证最新权限）。
     */
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

    /**
     * 修改密码（B1.4 Phase 2）。校验：
     * <ol>
     *   <li>用户存在 + 旧密码匹配</li>
     *   <li>新密码 ≠ 旧密码</li>
     *   <li>newPassword == confirmPassword</li>
     *   <li>新密码长度 ≥ 8（DTO 上 @Size 兜底；此处再校一遍防止有人绕过 valid）</li>
     * </ol>
     * 成功后立即把当前 access token 拉黑（迫使重新登录）。
     *
     * @param username      当前登录人
     * @param req           请求体
     * @param currentBearer 当前 Authorization 头（带 Bearer 前缀也可），用于拉黑当前 token
     */
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

        // 拉黑当前 access token，迫使重新登录
        revokeBearer(currentBearer);
    }

    /**
     * 刷新 token（B1.4 Phase 2）：
     * <ol>
     *   <li>校验 JWT 合法（签名 + 未过期）</li>
     *   <li>校验 {@code type == REFRESH}（access token 不能当 refresh 用）</li>
     *   <li>校验未在黑名单</li>
     *   <li>签发新 access + 新 refresh，并把旧 refresh 拉黑（防重放）</li>
     * </ol>
     *
     * <p>userInfo 字段不重新查 DB，复用 token 内 claim 拼一个简版（节省 IO）。
     */
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

        // 旧 refresh 拉黑（一次性使用，防重放）
        long ttl = jwtTokenProvider.getRemainingMillis(token);
        tokenBlacklist.revoke(claims.getId(), ttl);

        String newAccess = jwtTokenProvider.generateAccessToken(userId, username, roles);
        String newRefresh = jwtTokenProvider.generateRefreshToken(userId, username, roles);

        // userInfo 简版：从 claim 拼，不查 DB
        UserInfo info = UserInfo.builder()
                .id(userId)
                .username(username)
                .roles(roles)
                .build();
        return LoginResponse.builder()
                .accessToken(newAccess)
                .refreshToken(newRefresh)
                .userInfo(info)
                .build();
    }

    /** 拉黑一个 Bearer/裸 token；token 不可解析时静默忽略（与登出一致）。 */
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
                .roles(u.getRoles())
                .permissions(u.getPermissions())
                .build();
    }

    private static String stripBearer(String header) {
        if (header == null) {
            return null;
        }
        return header.startsWith("Bearer ") ? header.substring(7).trim() : header.trim();
    }
}
