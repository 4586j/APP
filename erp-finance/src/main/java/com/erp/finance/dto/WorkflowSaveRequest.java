package com.erp.finance.dto;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.math.BigDecimal; import java.util.List;
/** 工作流创建/更新请求（含节点，更新时整体覆盖节点） */
@Data
public class WorkflowSaveRequest {
    @NotBlank String workflowCode;
    @NotBlank String workflowName;
    String targetType;
    String description;
    Integer status;
    List<NodeReq> nodes;

    @Data
    public static class NodeReq {
        Integer nodeOrder; String nodeName; String approverRole;
        BigDecimal minAmount; BigDecimal maxAmount;
    }
}
