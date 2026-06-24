package com.erp.order.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal; import java.time.LocalDate; import java.time.LocalDateTime;
@Data @TableName("ord_purchase_order")
public class OrdPurchaseOrder {
    @TableId(type = IdType.AUTO) private Long id;
    private String orderNo; private Long supplierId; private Long relatedSalesOrderId;
    private LocalDate orderDate; private LocalDate expectedDelivery;
    private BigDecimal totalAmount; private String currency; private String paymentTerms;
    private String remarks; private String status;
    private Long createdBy; private Long updatedBy;
    @TableField(fill = FieldFill.INSERT) private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE) private LocalDateTime updatedAt;
    @TableLogic private Integer deleted;
}