package com.erp.finance.dto;
import lombok.Builder; import lombok.Data;
import java.time.LocalDateTime;
@Data @Builder
public class AppApprovalHistoryVO {
    Long id; Long requestId; Long approver; String action; String comment; LocalDateTime createdAt;
}