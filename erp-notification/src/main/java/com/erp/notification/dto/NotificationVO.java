package com.erp.notification.dto;
import lombok.Builder; import lombok.Data;
import java.time.LocalDateTime;
/** 通知视图（含当前用户的已读态） */
@Data @Builder
public class NotificationVO {
    Long id;                 // ntf_user_notification.id（接收记录 ID）
    Long notificationId;     // ntf_notification.id
    String title; String content; String type;
    String sourceType; Long sourceId;
    Integer isRead; LocalDateTime readAt; LocalDateTime createdAt;
}
