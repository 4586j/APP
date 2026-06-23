package com.erp.common.utils;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StringUtilsTest {

    @Test
    void isBlank_shouldHandleNullEmptyAndWhitespace() {
        assertThat(StringUtils.isBlank(null)).isTrue();
        assertThat(StringUtils.isBlank("")).isTrue();
        assertThat(StringUtils.isBlank("   ")).isTrue();
        assertThat(StringUtils.isBlank("a")).isFalse();
    }

    @Test
    void isNotBlank_isInverseOfIsBlank() {
        assertThat(StringUtils.isNotBlank(null)).isFalse();
        assertThat(StringUtils.isNotBlank("x")).isTrue();
    }

    @Test
    void defaultIfBlank_shouldFallbackWhenBlank() {
        assertThat(StringUtils.defaultIfBlank(null, "def")).isEqualTo("def");
        assertThat(StringUtils.defaultIfBlank("", "def")).isEqualTo("def");
        assertThat(StringUtils.defaultIfBlank("  ", "def")).isEqualTo("def");
        assertThat(StringUtils.defaultIfBlank("real", "def")).isEqualTo("real");
    }

    @Test
    void truncate_shouldAddEllipsisWhenOverLength() {
        assertThat(StringUtils.truncate("hello world", 5)).isEqualTo("hello...");
    }

    @Test
    void truncate_shouldReturnOriginalWhenShortEnough() {
        assertThat(StringUtils.truncate("hi", 5)).isEqualTo("hi");
        // 正好等长
        assertThat(StringUtils.truncate("hello", 5)).isEqualTo("hello");
    }

    @Test
    void truncate_shouldHandleNullAndEmpty() {
        assertThat(StringUtils.truncate(null, 5)).isNull();
        assertThat(StringUtils.truncate("", 5)).isEqualTo("");
    }

    @Test
    void maskMobile_shouldMaskMiddleFour() {
        assertThat(StringUtils.maskMobile("13812345678")).isEqualTo("138****5678");
    }

    @Test
    void maskMobile_shouldReturnOriginalWhenInvalid() {
        assertThat(StringUtils.maskMobile(null)).isNull();
        assertThat(StringUtils.maskMobile("12345")).isEqualTo("12345");
        assertThat(StringUtils.maskMobile("23812345678")).isEqualTo("23812345678");
        assertThat(StringUtils.maskMobile("138123456789")).isEqualTo("138123456789");
    }

    @Test
    void maskEmail_shouldMaskLocalPart() {
        assertThat(StringUtils.maskEmail("alice@example.com")).isEqualTo("a***e@example.com");
    }

    @Test
    void maskEmail_shouldReturnOriginalWhenInvalid() {
        assertThat(StringUtils.maskEmail(null)).isNull();
        assertThat(StringUtils.maskEmail("not-an-email")).isEqualTo("not-an-email");
    }

    @Test
    void maskEmail_shortLocalShouldStillBeMasked() {
        // 本地名只有 2 个字符
        assertThat(StringUtils.maskEmail("ab@x.com")).isEqualTo("a***@x.com");
    }

    @Test
    void camelToSnake_shouldConvert() {
        assertThat(StringUtils.camelToSnake("userName")).isEqualTo("user_name");
        assertThat(StringUtils.camelToSnake("UserName")).isEqualTo("user_name");
        assertThat(StringUtils.camelToSnake("id")).isEqualTo("id");
        assertThat(StringUtils.camelToSnake("")).isEqualTo("");
        assertThat(StringUtils.camelToSnake(null)).isNull();
    }

    @Test
    void snakeToCamel_shouldConvert() {
        assertThat(StringUtils.snakeToCamel("user_name")).isEqualTo("userName");
        assertThat(StringUtils.snakeToCamel("a_b_c")).isEqualTo("aBC");
        assertThat(StringUtils.snakeToCamel("id")).isEqualTo("id");
        assertThat(StringUtils.snakeToCamel("")).isEqualTo("");
        assertThat(StringUtils.snakeToCamel(null)).isNull();
    }
}
