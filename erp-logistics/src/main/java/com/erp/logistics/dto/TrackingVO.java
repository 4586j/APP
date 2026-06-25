package com.erp.logistics.dto;
import lombok.Builder; import lombok.Data;
import java.time.LocalDateTime;
@Data @Builder
public class TrackingVO {
    Long id; Long shipmentId; LocalDateTime trackingDate; String location;
    String eventCode; String description; String operator; LocalDateTime createdAt;
}