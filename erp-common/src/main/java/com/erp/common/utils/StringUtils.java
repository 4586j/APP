package com.erp.common.utils;

import cn.hutool.core.util.StrUtil;

import java.util.regex.Pattern;

/**
 * 字符串工具类。
 *
 * <p>项目内统一通过本类访问字符串相关能力，避免在业务代码中直接引用 hutool / commons-lang。
 */
public final class StringUtils {

    /** 简单邮箱正则（用于 mask 判定，非严格校验） */
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    /** 11 位手机号正则（中国大陆） */
    private static final Pattern MOBILE_PATTERN = Pattern.compile("^1\\d{10}$");

    private StringUtils() {
        throw new UnsupportedOperationException("工具类不可实例化");
    }

    /** 判空（null / "" / 全空白） */
    public static boolean isBlank(CharSequence cs) {
        return StrUtil.isBlank(cs);
    }

    /** 非空（与 {@link #isBlank} 相反） */
    public static boolean isNotBlank(CharSequence cs) {
        return !isBlank(cs);
    }

    /**
     * 当字符串为空白时返回默认值，否则原样返回。
     */
    public static String defaultIfBlank(String str, String defaultValue) {
        return isBlank(str) ? defaultValue : str;
    }

    /**
     * 截断字符串：长度超过 maxLen 时截断并追加 "..."。
     *
     * <p>null / 空字符串原样返回；maxLen &lt;= 0 时同样原样返回。
     */
    public static String truncate(String str, int maxLen) {
        if (str == null || str.isEmpty() || maxLen <= 0) {
            return str;
        }
        if (str.length() <= maxLen) {
            return str;
        }
        return str.substring(0, maxLen) + "...";
    }

    /**
     * 手机号脱敏：11 位手机号保留前 3 后 4，中间 4 位用 * 替换；不符合则原样返回。
     */
    public static String maskMobile(String mobile) {
        if (mobile == null || !MOBILE_PATTERN.matcher(mobile).matches()) {
            return mobile;
        }
        return mobile.substring(0, 3) + "****" + mobile.substring(7);
    }

    /**
     * 邮箱脱敏：保留首尾两个字符，中间用 *** 替换；不符合邮箱格式则原样返回。
     */
    public static String maskEmail(String email) {
        if (email == null || !EMAIL_PATTERN.matcher(email).matches()) {
            return email;
        }
        int at = email.indexOf('@');
        String name = email.substring(0, at);
        String domain = email.substring(at);
        if (name.length() <= 2) {
            // 只有 1~2 个字符的本地名，简单保留首字符
            return name.charAt(0) + "***" + domain;
        }
        return name.charAt(0) + "***" + name.charAt(name.length() - 1) + domain;
    }

    /**
     * 驼峰转下划线：userName → user_name。
     */
    public static String camelToSnake(String str) {
        if (isBlank(str)) {
            return str;
        }
        StringBuilder sb = new StringBuilder(str.length() + 8);
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (Character.isUpperCase(c)) {
                if (i > 0) {
                    sb.append('_');
                }
                sb.append(Character.toLowerCase(c));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * 下划线转驼峰：user_name → userName。
     */
    public static String snakeToCamel(String str) {
        if (isBlank(str)) {
            return str;
        }
        StringBuilder sb = new StringBuilder(str.length());
        boolean upperNext = false;
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (c == '_') {
                upperNext = true;
            } else if (upperNext) {
                sb.append(Character.toUpperCase(c));
                upperNext = false;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
