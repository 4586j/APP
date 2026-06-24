package com.erp.common.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DepartmentTest {

    @Test
    void fromCode_validCodes() {
        assertEquals(Department.SALES, Department.fromCode("SALES"));
        assertEquals(Department.MANAGEMENT, Department.fromCode("MANAGEMENT"));
    }

    @Test
    void fromCode_invalid_returnsNull() {
        assertNull(Department.fromCode("HR"));
        assertNull(Department.fromCode(""));
        assertNull(Department.fromCode(null));
    }

    @Test
    void getValue_equalsCode() {
        for (Department d : Department.values()) {
            assertEquals(d.getCode(), d.getValue());
        }
    }

    @Test
    void allDepartmentsHaveI18nNames() {
        for (Department d : Department.values()) {
            assertNotNull(d.getDesc());
            assertNotNull(d.getDescEn());
            assertFalse(d.getDesc().isBlank());
            assertFalse(d.getDescEn().isBlank());
        }
    }
}
