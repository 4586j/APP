package com.erp.notification.entity;
import com.baomidou.mybatisplus.annotation.TableName;
import com.erp.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
/**
 * 通知主表（B6 收尾）。一条通知可发给多个接收人，接收态在 ntf_user_notification。
 */
@Data @EqualsAndHashCode(callSuper = true)
@TableName("ntf_notification")
public class NtfNotification extends BaseEntity {
    /** 标题 */
    private String title;
    /** 内容 */
    private String content;
    /** 类型：system/approval/business */
    private String type;
    /** 业务来源类型，如 approval_request */
    private String sourceType;
    /** 业务来源 ID */
    private Long sourceId;
}
