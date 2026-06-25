package com.erp.notification.dto;
import lombok.Builder; import lombok.Data; import java.util.List;
@Data @Builder
public class NotificationPageVO {
    List<NotificationVO> records; long total; long size; long current;
}
