package com.erp.order.dto;
import lombok.Builder; import lombok.Data;
import java.time.LocalDateTime;
@Data @Builder
public class OrderStatusHistoryVO {
    Long id; String orderType; Long orderId;
    String fromStatus; String toStatus; String operatorName; String remark;
    LocalDateTime createdAt;
}