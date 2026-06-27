package com.erp.common.cache;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.function.Supplier;

/**
 * Redis 缓存操作模板，统一处理异常降级。
 *
 * <p>设计目标：
 * <ul>
 *   <li>Redis 不可用时降级查数据库，不阻断业务请求</li>
 *   <li>统一缓存 Key 前缀规范：{@code erp:{module}:{entity}:{id}}</li>
 *   <li>自动记录缓存命中/降级日志，便于排查</li>
 * </ul>
 *
 * <p>使用示例：
 * <pre>{@code
 * List<PermissionTreeNode> tree = cacheTemplate.get(
 *     "erp:user:permission:tree",
 *     Duration.ofMinutes(10),
 *     () -> buildTreeFromDb(),
 *     (value) -> toJson(value),
 *     (json) -> fromJson(json, new TypeReference<>() {})
 * );
 * }</pre>
 */
@Slf4j
public class CacheTemplate {

    private final StringRedisTemplate redisTemplate;

    public CacheTemplate(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 读取缓存，未命中时回源并写入缓存。
     *
     * @param key          Redis key
     * @param ttl          缓存过期时间
     * @param loader       缓存未命中时的回源函数
     * @param serializer   对象 → JSON 序列化
     * @param deserializer JSON → 对象反序列化
     * @return 缓存值或回源值
     */
    public <T> T get(String key, Duration ttl,
                     Supplier<T> loader,
                     java.util.function.Function<T, String> serializer,
                     java.util.function.Function<String, T> deserializer) {
        // 1. 尝试读缓存
        try {
            String cached = redisTemplate.opsForValue().get(key);
            if (cached != null) {
                T value = deserializer.apply(cached);
                if (value != null) {
                    return value;
                }
            }
        } catch (RedisConnectionFailureException e) {
            log.warn("Redis 连接失败，降级查库, key={}: {}", key, e.getMessage());
        } catch (Exception e) {
            log.warn("Redis 读缓存异常，降级查库, key={}: {}", key, e.getMessage());
        }

        // 2. 回源
        T value = loader.get();
        if (value == null) {
            return null;
        }

        // 3. 写入缓存（失败不抛异常）
        try {
            String json = serializer.apply(value);
            if (json != null) {
                redisTemplate.opsForValue().set(key, json, ttl);
            }
        } catch (RedisConnectionFailureException e) {
            log.warn("Redis 连接失败，跳过写缓存, key={}: {}", key, e.getMessage());
        } catch (Exception e) {
            log.warn("Redis 写缓存异常, key={}: {}", key, e.getMessage());
        }

        return value;
    }

    /**
     * 删除缓存，静默处理异常。
     */
    public void delete(String key) {
        try {
            redisTemplate.delete(key);
        } catch (Exception e) {
            log.warn("Redis 删除缓存异常, key={}: {}", key, e.getMessage());
        }
    }

    /**
     * 主动写入缓存，静默处理异常。
     *
     * @param key   Redis key
     * @param value 缓存值（会被序列化为 JSON）
     * @param ttl   过期时间
     */
    public void put(String key, Object value, Duration ttl) {
        try {
            String json = toJson(value);
            if (json != null) {
                redisTemplate.opsForValue().set(key, json, ttl);
            }
        } catch (RedisConnectionFailureException e) {
            log.warn("Redis 连接失败，跳过写缓存, key={}: {}", key, e.getMessage());
        } catch (Exception e) {
            log.warn("Redis 写缓存异常, key={}: {}", key, e.getMessage());
        }
    }

    private String toJson(Object value) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper()
                .findAndRegisterModules()
                .writeValueAsString(value);
        } catch (Exception e) {
            log.warn("缓存对象序列化失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 批量删除缓存（按 pattern 不推荐生产用，这里只支持精确 key 列表）。
     */
    public void delete(String... keys) {
        if (keys == null || keys.length == 0) return;
        try {
            redisTemplate.delete(java.util.Arrays.asList(keys));
        } catch (Exception e) {
            log.warn("Redis 批量删除缓存异常: {}", e.getMessage());
        }
    }
}
