package com.erp.logistics.entity;
import com.baomidou.mybatisplus.annotation.*; import lombok.Data;
import java.time.LocalDateTime;
@Data @TableName("log_tracking")
public class LogTracking {
    @TableId(type=IdType.AUTO) Long id; Long shipmentId;
    LocalDateTime trackingDate; String location; String eventCode;
    String description; String operator; LocalDateTime createdAt;
}