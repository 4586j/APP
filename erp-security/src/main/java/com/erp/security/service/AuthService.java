package com.erp.security.service;

import com.erp.common.exception.BusinessException;
import com.erp.common.model.R;
import com.erp.security.dto.LoginRequest;
import com.erp.security.dto.LoginResponse;
import com.erp.security.dto.UserInfo;
import com.erp.security.token.JwtTokenProvider;
import com.erp.security.token.TokenBlacklist;
import com.erp.security.user.LoginUser;
import com.erp.security.user.UserDetailsLoader;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

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

    public AuthService(UserDetailsLoader userDetailsLoader,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider jwtTokenProvider,
                       TokenBlacklist tokenBlacklist) {
        this.userDetailsLoader = userDetailsLoader;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.tokenBlacklist = tokenBlacklist;
    }

    /**
     * 登录：校验用户名 + BCrypt 密码，签发 access + refresh token。
     *
     * @throws BusinessException 用户不存在或密码错误（统一返回 401 错误码，避免账号枚举）
     */
    public LoginResponse login(LoginRequest req) {
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
