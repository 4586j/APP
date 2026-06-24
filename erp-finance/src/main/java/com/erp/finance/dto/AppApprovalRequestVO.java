package com.erp.finance.dto;
import lombok.Builder; import lombok.Data;
import java.math.BigDecimal; import java.time.LocalDateTime; import java.util.List;
@Data @Builder
public class AppApprovalRequestVO {
    Long id; String requestNo; Long workflowId; String targetType; Long targetId;
    String title; BigDecimal amount; String currency; String status; Long applicant;
    LocalDateTime createdAt;
    List<AppApprovalHistoryVO> history;
}