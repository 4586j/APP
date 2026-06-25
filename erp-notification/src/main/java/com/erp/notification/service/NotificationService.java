package com.erp.notification.service;
import com.erp.notification.dto.*;
import java.util.List;
public interface NotificationService {
    /** 创建通知并分发给接收人，返回通知主表 ID */
    Long create(NotificationCreateRequest r);
    /** 便捷方法：供其他模块（如审批）直接调用，发给单个接收人 */
    Long send(String title, String content, String type, String sourceType, Long sourceId, Long receiverId);
    /** 我的通知分页列表 */
    NotificationPageVO myNotifications(Long userId, NotificationQuery q);
    /** 标记单条已读（按接收记录 id） */
    void markRead(Long userNotificationId, Long userId);
    /** 批量标记已读；ids 为空表示全部已读 */
    int batchRead(List<Long> userNotificationIds, Long userId);
    /** 我的未读数量 */
    long unreadCount(Long userId);
}
