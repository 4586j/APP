package com.erp.order.dto;
import lombok.Builder; import lombok.Data;
import java.math.BigDecimal;
@Data @Builder
public class SalesOrderItemVO {
    Long id; Integer lineNo; Long productId; String productCode; String productName;
    String hsCode; String specification; BigDecimal quantity; String unit;
    BigDecimal unitPrice; BigDecimal totalPrice;
}