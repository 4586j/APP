package com.erp.order.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal; import java.time.LocalDate; import java.time.LocalDateTime;
@Data @TableName("ord_sales_order")
public class OrdSalesOrder {
    @TableId(type = IdType.AUTO) private Long id;
    private String orderNo; private Long customerId; private String customerOrderNo;
    private LocalDate orderDate; private String currency;
    private String tradeTerms; private String paymentTerms;
    private String portLoading; private String portDestination; private LocalDate expectedDelivery;
    private BigDecimal totalAmount; private BigDecimal totalCnyAmount; private BigDecimal exchangeRate;
    private String remarks; private String status;
    private Long createdBy; private Long updatedBy;
    @TableField(fill = FieldFill.INSERT) private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE) private LocalDateTime updatedAt;
    @TableLogic private Integer deleted;
}