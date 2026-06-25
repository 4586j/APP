package com.erp.finance.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal; import java.time.LocalDateTime;
@Data @TableName("fin_fund_approval")
public class FinFundApproval {
    @TableId(type = IdType.AUTO) private Long id;
    private String requestNo; private String title;
    private String fundType; private BigDecimal amount; private String currency;
    private Long supplierId; private String description;
    private String status; private Long applicant;
    /** 按金额阈值自动分配的审批角色 */
    private String approverRole;
    /** 实际审批人ID */
    private Long approver;
    /** 审批备注 */
    private String approveComment;
    @TableField(fill = FieldFill.INSERT) private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE) private LocalDateTime updatedAt;
    @Version private Integer version;
    @TableLogic private Integer deleted;
}