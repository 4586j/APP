package com.erp.security.token;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * JWT 生成与解析工具（B1.4 Phase 1）。
 *
 * <p>HS256 签名，密钥从 {@code app.security.jwt.secret} 读取（至少 32 字节）。
 * Access token 默认 30 分钟，Refresh token 默认 30 天，可在 yaml 覆盖。
 */
@Slf4j
@Component
public class JwtTokenProvider {

    /** Claim：用户 ID。 */
    public static final String CLAIM_USER_ID = "userId";

    /** Claim：登录名。 */
    public static final String CLAIM_USERNAME = "username";

    /** Claim：角色列表。 */
    public static final String CLAIM_ROLES = "roles";

    /** Claim：token 类型（access/refresh）。 */
    public static final String CLAIM_TYPE = "type";

    private final String secret;
    private final long accessExpireMillis;
    private final long refreshExpireMillis;

    private SecretKey signingKey;

    public JwtTokenProvider(
            @Value("${app.security.jwt.secret}") String secret,
            @Value("${app.security.jwt.access-token-expire-minutes:30}") long accessExpireMinutes,
            @Value("${app.security.jwt.refresh-token-expire-days:30}") long refreshExpireDays) {
        this.secret = secret;
        this.accessExpireMillis = accessExpireMinutes * 60L * 1000L;
        this.refreshExpireMillis = refreshExpireDays * 24L * 60L * 60L * 1000L;
    }

    @PostConstruct
    void init() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalStateException(
                    "app.security.jwt.secret 长度不足 32 字节（HS256 要求 ≥256 bit）");
        }
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
        log.info("JwtTokenProvider 已初始化，access={}min, refresh={}d",
                accessExpireMillis / 60_000, refreshExpireMillis / 86_400_000);
    }

    /**
     * 生成 access token。
     *
     * @param userId   用户 ID
     * @param username 登录名
     * @param roles    角色编码列表
     * @return JWT 字符串
     */
    public String generateAccessToken(Long userId, String username, List<String> roles) {
        return buildToken(userId, username, roles, TokenType.ACCESS, accessExpireMillis);
    }

    /**
     * 生成 refresh token。
     */
    public String generateRefreshToken(Long userId, String username, List<String> roles) {
        return buildToken(userId, username, roles, TokenType.REFRESH, refreshExpireMillis);
    }

    private String buildToken(Long userId, String username, List<String> roles,
                              TokenType type, long ttlMillis) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(username)
                .issuedAt(new Date(now))
                .expiration(new Date(now + ttlMillis))
                .claims(Map.of(
                        CLAIM_USER_ID, userId,
                        CLAIM_USERNAME, username,
                        CLAIM_ROLES, roles == null ? List.of() : roles,
                        CLAIM_TYPE, type.name()
                ))
                .signWith(signingKey)
                .compact();
    }

    /**
     * 解析 token；签名错误 / 解码失败 / 已过期都抛 {@link JwtException}。
     */
    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 判断 token 是否已过期；解析异常或过期均视为 true。
     */
    public boolean isExpired(String token) {
        try {
            Date exp = parseToken(token).getExpiration();
            return exp == null || exp.before(new Date());
        } catch (ExpiredJwtException e) {
            return true;
        } catch (JwtException e) {
            return true;
        }
    }

    /**
     * 取 token 的类型 claim。
     */
    public TokenType getTokenType(String token) {
        String type = parseToken(token).get(CLAIM_TYPE, String.class);
        return type == null ? TokenType.ACCESS : TokenType.valueOf(type);
    }

    /**
     * 取 token 剩余 TTL（毫秒）。已过期返回 0。
     */
    public long getRemainingMillis(String token) {
        try {
            Date exp = parseToken(token).getExpiration();
            long remain = exp.getTime() - System.currentTimeMillis();
            return Math.max(0L, remain);
        } catch (JwtException e) {
            return 0L;
        }
    }
}
