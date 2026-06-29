package com.erp.data.webdav;

import lombok.AllArgsConstructor;
import lombok.Data;

/** WebDAV 锁信息（独占）。 */
@Data
@AllArgsConstructor
public class WebDavLockInfo {
    private Long fileId;
    private Long ownerUserId;
    private String token;
    private long expireAt; // epoch millis
}