package com.erp.finance.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
@Data @TableName("app_approval_request")
public class AppApprovalRequest {
    @TableId(type=IdType.AUTO) Long id;
    String requestNo;
    Long workflowId;
    String targetType;
    Long targetId;
    String title;
    BigDecimal amount;
    String currency="CNY";
    String status="pending";
    Long applicant;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}