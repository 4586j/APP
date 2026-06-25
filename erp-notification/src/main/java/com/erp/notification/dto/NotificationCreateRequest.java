package com.erp.notification.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import java.util.List;
/** 创建通知请求 */
@Data
public class NotificationCreateRequest {
    @NotBlank String title;
    @NotBlank String content;
    /** system/approval/business */
    @NotBlank String type;
    String sourceType; Long sourceId;
    /** 接收人用户 ID 列表 */
    @NotEmpty List<Long> receiverIds;
}
