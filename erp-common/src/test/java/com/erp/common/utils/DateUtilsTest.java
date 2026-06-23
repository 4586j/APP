package com.erp.common.utils;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DateUtilsTest {

    @Test
    void now_shouldReturnNonNull() {
        LocalDateTime now = DateUtils.now();
        assertThat(now).isNotNull();
    }

    @Test
    void today_shouldReturnLocalDate() {
        LocalDate today = DateUtils.today();
        assertThat(today).isNotNull();
        assertThat(today.getYear()).isGreaterThanOrEqualTo(2024);
    }

    @Test
    void format_shouldFormatWithGivenPattern() {
        LocalDateTime t = LocalDateTime.of(2026, 6, 24, 10, 30, 45);
        assertThat(DateUtils.format(t, DateUtils.PATTERN_DATETIME)).isEqualTo("2026-06-24 10:30:45");
        assertThat(DateUtils.format(t, DateUtils.PATTERN_DATE)).isEqualTo("2026-06-24");
    }

    @Test
    void format_shouldReturnNullForNullInput() {
        assertThat(DateUtils.format(null, DateUtils.PATTERN_DATETIME)).isNull();
    }

    @Test
    void parse_shouldParseWithGivenPattern() {
        LocalDateTime t = DateUtils.parse("2026-06-24 10:30:45", DateUtils.PATTERN_DATETIME);
        assertThat(t).isEqualTo(LocalDateTime.of(2026, 6, 24, 10, 30, 45));
    }

    @Test
    void toMillis_fromMillis_roundTripShouldBeConsistent() {
        LocalDateTime t = LocalDateTime.of(2026, 6, 24, 10, 30, 45);
        long millis = DateUtils.toMillis(t);
        LocalDateTime back = DateUtils.fromMillis(millis);
        assertThat(back).isEqualTo(t);
    }

    @Test
    void toMillis_shouldRespectShanghaiZone() {
        // 同一墙上时间，UTC 时区比 Asia/Shanghai 晚 8 小时
        LocalDateTime t = LocalDateTime.of(2026, 6, 24, 0, 0, 0);
        long cnMillis = DateUtils.toMillis(t);
        long utcMillis = t.atZone(java.time.ZoneOffset.UTC).toInstant().toEpochMilli();
        assertThat(utcMillis - cnMillis).isEqualTo(8 * 3600 * 1000L);
    }

    @Test
    void constructor_shouldNotBeInstantiable() throws NoSuchMethodException {
        Constructor<DateUtils> ctor = DateUtils.class.getDeclaredConstructor();
        assertThat(Modifier.isPrivate(ctor.getModifiers())).isTrue();
        ctor.setAccessible(true);
        assertThatThrownBy(ctor::newInstance)
                .isInstanceOf(InvocationTargetException.class)
                .hasCauseInstanceOf(UnsupportedOperationException.class);
    }
}
