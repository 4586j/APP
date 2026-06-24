package com.erp.product.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal; import java.time.LocalDateTime;
@Data @TableName("prd_hs_code")
public class PrdHsCode {
    @TableId(type = IdType.AUTO) private Long id;
    private String hsCode; private String description;
    private BigDecimal tariffRate; private BigDecimal vatRate; private BigDecimal exportRefundRate;
    private String restrictions;
    @TableField(fill = FieldFill.INSERT) private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE) private LocalDateTime updatedAt;
}