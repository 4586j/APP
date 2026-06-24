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
    @TableField(fill = FieldFill.INSERT) private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE) private LocalDateTime updatedAt;
    @TableLogic private Integer deleted;
}