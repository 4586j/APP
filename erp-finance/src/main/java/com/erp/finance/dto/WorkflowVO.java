package com.erp.finance.dto;
import lombok.Builder; import lombok.Data;
import java.math.BigDecimal; import java.time.LocalDateTime; import java.util.List;
/** 工作流定义（含节点） */
@Data @Builder
public class WorkflowVO {
    Long id; String workflowCode; String workflowName; String targetType;
    String description; Integer status; LocalDateTime createdAt;
    List<NodeVO> nodes;

    @Data @Builder
    public static class NodeVO {
        Long id; Long workflowId; Integer nodeOrder; String nodeName;
        String approverRole; BigDecimal minAmount; BigDecimal maxAmount;
    }
}
