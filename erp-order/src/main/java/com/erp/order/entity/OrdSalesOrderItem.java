package com.erp.order.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal; import java.time.LocalDateTime;
@Data @TableName("ord_sales_order_item")
public class OrdSalesOrderItem {
    @TableId(type = IdType.AUTO) private Long id;
    private Long orderId; private Integer lineNo;
    private Long productId; private String productCode; private String productName;
    private String hsCode; private String specification;
    private BigDecimal quantity; private String unit;
    private BigDecimal unitPrice; private BigDecimal totalPrice;
    @TableField(fill = FieldFill.INSERT) private LocalDateTime createdAt;
}