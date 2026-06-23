package com.erp.common.utils;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

class IdGeneratorTest {

    private static final Pattern DIGITS = Pattern.compile("^\\d+$");
    private static final Pattern HEX_32 = Pattern.compile("^[0-9a-f]{32}$");

    @Test
    void nextId_shouldNotRepeatIn10000Calls() {
        Set<Long> ids = new HashSet<>(10000);
        for (int i = 0; i < 10000; i++) {
            ids.add(IdGenerator.nextId());
        }
        assertThat(ids).hasSize(10000);
    }

    @Test
    void nextId_shouldBeMonotonicallyIncreasing() {
        long prev = IdGenerator.nextId();
        for (int i = 0; i < 1000; i++) {
            long curr = IdGenerator.nextId();
            assertThat(curr).isGreaterThan(prev);
            prev = curr;
        }
    }

    @Test
    void nextIdStr_shouldBeAllDigits() {
        String id = IdGenerator.nextIdStr();
        assertThat(id).isNotBlank();
        assertThat(DIGITS.matcher(id).matches()).isTrue();
    }

    @Test
    void nextUuid_shouldBe32HexChars() {
        String uuid = IdGenerator.nextUuid();
        assertThat(uuid).hasSize(32);
        assertThat(HEX_32.matcher(uuid).matches()).isTrue();
    }
}
