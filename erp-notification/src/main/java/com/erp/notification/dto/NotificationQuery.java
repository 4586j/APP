package com.erp.notification.dto;
import lombok.Data;
/** 我的通知查询 */
@Data
public class NotificationQuery {
    /** system/approval/business，可空=全部 */
    private String type;
    /** 0=未读 1=已读，可空=全部 */
    private Integer isRead;
    private Integer page = 1;
    private Integer size = 20;
}
