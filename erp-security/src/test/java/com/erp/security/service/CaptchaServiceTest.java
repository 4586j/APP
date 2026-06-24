package com.erp.security.service;

import com.erp.security.captcha.CaptchaProperties;
import com.erp.security.dto.CaptchaResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * CaptchaService 单元测试（B1.4 Phase 2）。
 *
 * <p>Redis 用 Map 模拟（StringRedisTemplate + ValueOperations 全 mock），
 * 覆盖：生成、校验通过、一次性消费、过期/不存在视为失败。
 */
class CaptchaServiceTest {

    private CaptchaService service;
    private Map<String, String> redisFake;
    private StringRedisTemplate redisTemplate;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        redisFake = new HashMap<>();
        redisTemplate = mock(StringRedisTemplate.class);
        ValueOperations<String, String> valueOps = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);

        // set(key, value, ttl, unit) → 存入 map（忽略 TTL，由"删除"模拟过期）
        doAnswer(inv -> {
            redisFake.put(inv.getArgument(0), inv.getArgument(1));
            return null;
        }).when(valueOps).set(any(String.class), any(String.class), anyLong(), any(TimeUnit.class));

        when(valueOps.get(any(String.class)))
                .thenAnswer(inv -> redisFake.get((String) inv.getArgument(0)));

        when(redisTemplate.delete(any(String.class)))
                .thenAnswer(inv -> redisFake.remove((String) inv.getArgument(0)) != null);

        CaptchaProperties props = new CaptchaProperties();
        props.setEnabled(true);
        props.setLength(4);
        props.setTtlMinutes(5);
        service = new CaptchaService(redisTemplate, props);
    }

    @Test
    void generate_returnsUuidAndPng() {
        CaptchaResponse resp = service.generate();
        assertNotNull(resp.getUuid());
        assertTrue(resp.getImageBase64().startsWith("data:image/png;base64,"),
                "imageBase64 必须带 data URL 前缀");
        assertTrue(redisFake.containsKey("auth:captcha:" + resp.getUuid()),
                "答案应已写入 Redis");
    }

    @Test
    void verify_correctAnswer_returnsTrue_caseInsensitive() {
        CaptchaResponse resp = service.generate();
        String key = "auth:captcha:" + resp.getUuid();
        String expected = redisFake.get(key);
        // 大小写互换仍应通过
        assertTrue(service.verify(resp.getUuid(), expected.toLowerCase()),
                "校验应大小写不敏感");
    }

    @Test
    void verify_isOneShot_consumesKey() {
        CaptchaResponse resp = service.generate();
        String key = "auth:captcha:" + resp.getUuid();
        String expected = redisFake.get(key);

        assertTrue(service.verify(resp.getUuid(), expected));
        assertFalse(redisFake.containsKey(key), "校验后必须删 key");

        // 再次校验，必须失败（一次性）
        assertFalse(service.verify(resp.getUuid(), expected));
    }

    @Test
    void verify_unknownUuid_returnsFalse() {
        assertFalse(service.verify("does-not-exist", "ABCD"));
    }

    @Test
    void verify_emptyInputs_returnsFalse() {
        assertFalse(service.verify(null, "ABCD"));
        assertFalse(service.verify("any", ""));
        assertFalse(service.verify("", "any"));
    }
}
