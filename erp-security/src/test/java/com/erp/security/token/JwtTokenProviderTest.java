package com.erp.security.token;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * JwtTokenProvider 单元测试。
 *
 * <p>覆盖：往返一致、过期检测、token 类型识别、不同密钥签名互不通过。
 */
class JwtTokenProviderTest {

    private static final String SECRET =
            "unit-test-secret-key-which-is-long-enough-for-hs256-XXXXXXXXXXXXXXX";

    private JwtTokenProvider provider;

    @BeforeEach
    void setUp() {
        provider = new JwtTokenProvider(SECRET, 30, 30);
        invokeInit(provider);
    }

    @Test
    void generateAndParse_accessToken_roundTrip() {
        String token = provider.generateAccessToken(42L, "alice", List.of("ROLE_ADMIN"));
        assertNotNull(token);

        Claims claims = provider.parseToken(token);
        Number userId = (Number) claims.get(JwtTokenProvider.CLAIM_USER_ID);
        assertEquals(42L, userId.longValue());
        assertEquals("alice", claims.get(JwtTokenProvider.CLAIM_USERNAME, String.class));
        assertEquals("ACCESS", claims.get(JwtTokenProvider.CLAIM_TYPE, String.class));
        assertEquals(TokenType.ACCESS, provider.getTokenType(token));
        assertFalse(provider.isExpired(token));
        assertTrue(provider.getRemainingMillis(token) > 0);
    }

    @Test
    void generateRefresh_typeIsRefresh() {
        String refresh = provider.generateRefreshToken(7L, "bob", List.of("ROLE_USER"));
        assertEquals(TokenType.REFRESH, provider.getTokenType(refresh));
    }

    @Test
    void expiredToken_isDetected() throws Exception {
        // 用 0 分钟 access 直接拿到一个已过期 token
        JwtTokenProvider shortLived = new JwtTokenProvider(SECRET, 0, 0);
        invokeInit(shortLived);
        String token = shortLived.generateAccessToken(1L, "x", List.of());
        // 留点时间确保越过当前 ms
        Thread.sleep(10);

        assertTrue(shortLived.isExpired(token));
        assertEquals(0L, shortLived.getRemainingMillis(token));
        assertThrows(ExpiredJwtException.class, () -> shortLived.parseToken(token));
    }

    @Test
    void differentSecret_failsSignatureValidation() {
        String token = provider.generateAccessToken(1L, "x", List.of());

        JwtTokenProvider other = new JwtTokenProvider(
                "different-secret-but-also-long-enough-for-hs256-padding-AAAAAAAA",
                30, 30);
        invokeInit(other);
        assertThrows(JwtException.class, () -> other.parseToken(token));
        assertTrue(other.isExpired(token), "异常签名应被视为过期/不可信");
    }

    @Test
    void tooShortSecret_failsInit() {
        JwtTokenProvider bad = new JwtTokenProvider("short", 30, 30);
        assertThrows(IllegalStateException.class, () -> invokeInit(bad));
    }

    /** 反射触发 {@code @PostConstruct init()}（包私有方法）。 */
    private static void invokeInit(JwtTokenProvider provider) {
        try {
            Method m = JwtTokenProvider.class.getDeclaredMethod("init");
            m.setAccessible(true);
            m.invoke(provider);
        } catch (ReflectiveOperationException e) {
            if (e.getCause() instanceof RuntimeException re) {
                throw re;
            }
            throw new RuntimeException(e);
        }
        // 也帮 ReflectionTestUtils 校验 signingKey 字段已被设置（仅在初始化成功路径）
        assertNotNull(ReflectionTestUtils.getField(provider, "signingKey"));
    }
}
