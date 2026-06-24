package com.erp.product.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal; import java.time.LocalDate; import java.time.LocalDateTime;
@Data @TableName("prd_product_price")
public class PrdProductPrice {
    @TableId(type = IdType.AUTO) private Long id;
    private Long productId; private String priceType; private String currencyCode; private BigDecimal price;
    private LocalDate validFrom; private LocalDate validTo;
    @TableField(fill = FieldFill.INSERT) private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE) private LocalDateTime updatedAt;
}