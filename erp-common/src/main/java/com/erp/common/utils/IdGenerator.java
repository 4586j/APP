package com.erp.common.utils;

import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.IdUtil;

/**
 * 全局 ID 生成器（雪花算法）。
 *
 * <p>workerId / datacenterId 由环境变量 {@code WORKER_ID} / {@code DATACENTER_ID} 控制，
 * 缺省均为 0。底层基于 hutool 的 {@link IdUtil} / {@link Snowflake}。
 */
public final class IdGenerator {

    private static final long WORKER_ID = readLongEnv("WORKER_ID", 0L);
    private static final long DATACENTER_ID = readLongEnv("DATACENTER_ID", 0L);

    /** 单例 Snowflake 实例 */
    private static final Snowflake SNOWFLAKE = IdUtil.getSnowflake(WORKER_ID, DATACENTER_ID);

    private IdGenerator() {
        throw new UnsupportedOperationException("工具类不可实例化");
    }

    /** 下一个雪花 ID（long） */
    public static long nextId() {
        return SNOWFLAKE.nextId();
    }

    /** 下一个雪花 ID（数字字符串） */
    public static String nextIdStr() {
        return SNOWFLAKE.nextIdStr();
    }

    /** 一个无连字符的 UUID（32 位 hex） */
    public static String nextUuid() {
        return IdUtil.simpleUUID();
    }

    private static long readLongEnv(String key, long defaultValue) {
        String value = System.getenv(key);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
