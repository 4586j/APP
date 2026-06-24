package com.erp.common.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OrderStatusTest {

    @Test
    void fromCode_validCodes() {
        assertEquals(OrderStatus.DRAFT, OrderStatus.fromCode(10));
        assertEquals(OrderStatus.SHIPPED, OrderStatus.fromCode(60));
        assertEquals(OrderStatus.CANCELLED, OrderStatus.fromCode(99));
    }

    @Test
    void fromCode_invalid_returnsNull() {
        assertNull(OrderStatus.fromCode(0));
        assertNull(OrderStatus.fromCode(15));
        assertNull(OrderStatus.fromCode(null));
    }

    @Test
    void isTerminal() {
        assertTrue(OrderStatus.COMPLETED.isTerminal());
        assertTrue(OrderStatus.CLOSED.isTerminal());
        assertTrue(OrderStatus.CANCELLED.isTerminal());
        assertFalse(OrderStatus.DRAFT.isTerminal());
        assertFalse(OrderStatus.SHIPPED.isTerminal());
    }

    @Test
    void isActive() {
        assertTrue(OrderStatus.DRAFT.isActive());
        assertTrue(OrderStatus.IN_PRODUCTION.isActive());
        assertFalse(OrderStatus.COMPLETED.isActive());
        assertFalse(OrderStatus.CANCELLED.isActive());
    }

    @Test
    void canTransition_forwardOnly() {
        assertTrue(OrderStatus.DRAFT.canTransitionTo(OrderStatus.PENDING_APPROVAL));
        assertTrue(OrderStatus.APPROVED.canTransitionTo(OrderStatus.IN_PRODUCTION));
        assertTrue(OrderStatus.SHIPPED.canTransitionTo(OrderStatus.DELIVERED));
    }

    @Test
    void canTransition_backwardRejected() {
        assertFalse(OrderStatus.IN_PRODUCTION.canTransitionTo(OrderStatus.DRAFT));
        assertFalse(OrderStatus.SHIPPED.canTransitionTo(OrderStatus.APPROVED));
    }

    @Test
    void canTransition_anyActiveCanCancel() {
        assertTrue(OrderStatus.DRAFT.canTransitionTo(OrderStatus.CANCELLED));
        assertTrue(OrderStatus.APPROVED.canTransitionTo(OrderStatus.CANCELLED));
        assertTrue(OrderStatus.SHIPPED.canTransitionTo(OrderStatus.CANCELLED));
    }

    @Test
    void canTransition_terminalRejected() {
        assertFalse(OrderStatus.COMPLETED.canTransitionTo(OrderStatus.CLOSED));
        assertFalse(OrderStatus.CANCELLED.canTransitionTo(OrderStatus.DRAFT));
        assertFalse(OrderStatus.CLOSED.canTransitionTo(OrderStatus.COMPLETED));
    }

    @Test
    void canTransition_selfRejected() {
        for (OrderStatus s : OrderStatus.values()) {
            assertFalse(s.canTransitionTo(s), "self-transition should fail: " + s);
        }
    }

    @Test
    void canTransition_nullRejected() {
        assertFalse(OrderStatus.DRAFT.canTransitionTo(null));
    }

    @Test
    void getValue_equalsCode() {
        for (OrderStatus s : OrderStatus.values()) {
            assertEquals(s.getCode(), s.getValue());
        }
    }
}
