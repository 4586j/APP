package com.erp.security.token;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Token 黑名单管理（B1.4 Phase 1）。
 *
 * <p>登出时将 jti 写入 Redis 黑名单，过期时间 = token 剩余 TTL。
 * 过滤器在校验通过后查询此处，命中即拒绝。
 *
 * <p>Redis 通讯失败时 fail-open（仅放过查询，不影响登入流程）；
 * 这是开发期与测试期的权衡，生产可以替换为 fail-close 策略。
 */
@Slf4j
@Component
public class TokenBlacklist {

    /** Redis key 前缀。 */
    public static final String KEY_PREFIX = "auth:blacklist:";

    private final StringRedisTemplate redisTemplate;

    public TokenBlacklist(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 拉黑一个 jti，过期时间随 token 剩余 TTL。
     */
    public void revoke(String jti, long ttlMillis) {
        if (jti == null || ttlMillis <= 0) {
            return;
        }
        try {
            redisTemplate.opsForValue().set(KEY_PREFIX + jti, "1", ttlMillis, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            log.warn("写入 Redis 黑名单失败: {}", e.getMessage());
        }
    }

    /**
     * 判断 jti 是否在黑名单中；Redis 不可用时一律返回 false（fail-open）。
     */
    public boolean isRevoked(String jti) {
        if (jti == null) {
            return false;
        }
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(KEY_PREFIX + jti));
        } catch (Exception e) {
            log.warn("查询 Redis 黑名单失败: {}", e.getMessage());
            return false;
        }
    }
}
