package com.erp.data.webdav;

import com.erp.common.exception.BusinessException;
import com.erp.common.model.R;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WebDavLockStoreTest {

    private final WebDavLockStore store = new WebDavLockStore();

    @Test
    void tryLock_success_returnsToken() {
        assertNotNull(store.tryLock(1L, 100L, 60));
    }

    @Test
    void tryLock_alreadyLockedByOther_returnsNull() {
        store.tryLock(1L, 100L, 60);
        assertNull(store.tryLock(1L, 200L, 60));
    }

    @Test
    void tryLock_sameOwner_relock_returnsToken() {
        store.tryLock(1L, 100L, 60);
        assertNotNull(store.tryLock(1L, 100L, 60));
    }

    @Test
    void unlock_correctToken_succeeds() {
        String token = store.tryLock(1L, 100L, 60);
        assertTrue(store.unlock(1L, token));
        assertNotNull(store.tryLock(1L, 200L, 60));
    }

    @Test
    void unlock_wrongToken_returnsFalse() {
        store.tryLock(1L, 100L, 60);
        assertFalse(store.unlock(1L, "wrong"));
    }

    @Test
    void isLockedByOther_otherHolds_returnsTrue() {
        store.tryLock(1L, 100L, 60);
        assertTrue(store.isLockedByOther(1L, 200L));
    }

    @Test
    void isLockedByOther_selfHolds_returnsFalse() {
        store.tryLock(1L, 100L, 60);
        assertFalse(store.isLockedByOther(1L, 100L));
    }

    @Test
    void assertLockHeld_lockedWrongToken_throws423() {
        store.tryLock(1L, 100L, 60);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> store.assertLockHeld(1L, "wrong"));
        assertEquals(R.CODE_LOCKED, ex.getCode());
    }

    @Test
    void assertLockHeld_notLocked_passes() {
        assertDoesNotThrow(() -> store.assertLockHeld(1L, null));
    }

    @Test
    void tryLock_expired_reclaimed() throws InterruptedException {
        store.tryLock(1L, 100L, 1);
        Thread.sleep(1100);
        assertNotNull(store.tryLock(1L, 200L, 60));
    }
}
