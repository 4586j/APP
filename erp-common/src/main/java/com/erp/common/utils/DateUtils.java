package com.erp.common.utils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * 日期时间工具类。
 *
 * <p>项目统一时区为 Asia/Shanghai；统一基于 java.time API，禁止使用 Date/Calendar 老 API。
 */
public final class DateUtils {

    /** 项目统一时区 */
    public static final ZoneId ZONE_CN = ZoneId.of("Asia/Shanghai");

    /** 标准日期时间格式 */
    public static final String PATTERN_DATETIME = "yyyy-MM-dd HH:mm:ss";

    /** 标准日期格式 */
    public static final String PATTERN_DATE = "yyyy-MM-dd";

    private DateUtils() {
        throw new UnsupportedOperationException("工具类不可实例化");
    }

    /** 当前时间（Asia/Shanghai 时区） */
    public static LocalDateTime now() {
        return LocalDateTime.now(ZONE_CN);
    }

    /** 当前日期（Asia/Shanghai 时区） */
    public static LocalDate today() {
        return LocalDate.now(ZONE_CN);
    }

    /**
     * 按给定 pattern 格式化时间。
     *
     * @param time    时间，null 返回 null
     * @param pattern 格式
     */
    public static String format(LocalDateTime time, String pattern) {
        if (time == null) {
            return null;
        }
        return DateTimeFormatter.ofPattern(pattern).format(time);
    }

    /**
     * 按给定 pattern 解析字符串为 LocalDateTime。
     *
     * @param text    待解析字符串
     * @param pattern 格式
     */
    public static LocalDateTime parse(String text, String pattern) {
        return LocalDateTime.parse(text, DateTimeFormatter.ofPattern(pattern));
    }

    /**
     * 转毫秒时间戳（按 Asia/Shanghai 时区解释入参）。
     */
    public static long toMillis(LocalDateTime time) {
        return time.atZone(ZONE_CN).toInstant().toEpochMilli();
    }

    /**
     * 由毫秒时间戳还原为 LocalDateTime（按 Asia/Shanghai 时区）。
     */
    public static LocalDateTime fromMillis(long millis) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), ZONE_CN);
    }
}
