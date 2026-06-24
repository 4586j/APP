package com.erp.finance.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
@Data @TableName("app_workflow_node")
public class AppWorkflowNode {
    @TableId(type=IdType.AUTO) Long id;
    Long workflowId;
    Integer nodeOrder;
    String nodeName;
    String approverRole;
    BigDecimal minAmount;
    BigDecimal maxAmount;
    LocalDateTime createdAt;
}