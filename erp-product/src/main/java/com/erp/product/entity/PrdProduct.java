package com.erp.product.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal; import java.time.LocalDateTime;
@Data @TableName("prd_product")
public class PrdProduct {
    @TableId(type = IdType.AUTO) private Long id;
    private String productCode; private String productName; private String productNameEn;
    private Long categoryId; private Long hsCodeId; private String hsCode;
    private String unit; private String specification; private String originCountry; private String brand;
    private BigDecimal purchasePrice; private BigDecimal salesPrice; private BigDecimal costPrice;
    private BigDecimal weightKg; private BigDecimal volumeCbm;
    private Integer moq; private Integer status;
    private Long createdBy; private Long updatedBy;
    @TableField(fill = FieldFill.INSERT) private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE) private LocalDateTime updatedAt;
    @TableLogic private Integer deleted;
}