package com.erp.common.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CurrencyTest {

    @Test
    void fromCode_validCodes() {
        assertEquals(Currency.USD, Currency.fromCode("USD"));
        assertEquals(Currency.JPY, Currency.fromCode("JPY"));
    }

    @Test
    void fromCode_invalid_returnsNull() {
        assertNull(Currency.fromCode("XXX"));
        assertNull(Currency.fromCode(null));
    }

    @Test
    void zeroFractionCurrencies() {
        assertEquals(0, Currency.JPY.getFractionDigits());
        assertEquals(0, Currency.KRW.getFractionDigits());
    }

    @Test
    void twoFractionCurrencies() {
        assertEquals(2, Currency.CNY.getFractionDigits());
        assertEquals(2, Currency.USD.getFractionDigits());
        assertEquals(2, Currency.EUR.getFractionDigits());
    }

    @Test
    void allCurrenciesHaveSymbol() {
        for (Currency c : Currency.values()) {
            assertNotNull(c.getSymbol());
            assertFalse(c.getSymbol().isBlank());
        }
    }
}
