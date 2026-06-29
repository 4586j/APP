package com.erp.data.webdav;

import com.erp.common.exception.BusinessException;
import com.erp.common.model.R;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** 内存独占锁存储（单机；多机后续换 Redis）。 */
@Component
public class WebDavLockStore {

    private final ConcurrentHashMap<Long, WebDavLockInfo> locks = new ConcurrentHashMap<>();

    public synchronized String tryLock(Long fileId, Long ownerUserId, long ttlSeconds) {
        purgeExpired(fileId);
        WebDavLockInfo existing = locks.get(fileId);
        if (existing != null && !existing.getOwnerUserId().equals(ownerUserId)) {
            return null;
        }
        String token = "opaquelocktoken:" + UUID.randomUUID();
        long expireAt = System.currentTimeMillis() + ttlSeconds * 1000L;
        locks.put(fileId, new WebDavLockInfo(fileId, ownerUserId, token, expireAt));
        return token;
    }

    public synchronized boolean unlock(Long fileId, String token) {
        WebDavLockInfo info = locks.get(fileId);
        if (info == null) return false;
        if (token == null || !token.equals(info.getToken())) return false;
        locks.remove(fileId);
        return true;
    }

    public synchronized boolean isLockedByOther(Long fileId, Long userId) {
        purgeExpired(fileId);
        WebDavLockInfo info = locks.get(fileId);
        return info != null && !info.getOwnerUserId().equals(userId);
    }

    public synchronized void assertLockHeld(Long fileId, String token) {
        purgeExpired(fileId);
        WebDavLockInfo info = locks.get(fileId);
        if (info == null) return;
        if (token == null || !token.equals(info.getToken())) {
            throw new BusinessException(R.CODE_LOCKED, "文件已被他人锁定");
        }
    }

    private void purgeExpired(Long fileId) {
        WebDavLockInfo info = locks.get(fileId);
        if (info != null && System.currentTimeMillis() > info.getExpireAt()) {
            locks.remove(fileId);
        }
    }
}
