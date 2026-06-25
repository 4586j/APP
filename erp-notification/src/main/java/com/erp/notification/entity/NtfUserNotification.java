package com.erp.notification.entity;
import com.baomidou.mybatisplus.annotation.TableName;
import com.erp.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;
/**
 * 用户通知关联表（B6 收尾）。记录每个接收人的已读状态。
 */
@Data @EqualsAndHashCode(callSuper = true)
@TableName("ntf_user_notification")
public class NtfUserNotification extends BaseEntity {
    /** → ntf_notification.id */
    private Long notificationId;
    /** 接收人用户 ID */
    private Long userId;
    /** 是否已读：0=未读 1=已读 */
    private Integer isRead;
    /** 已读时间 */
    private LocalDateTime readAt;
}
