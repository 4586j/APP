package com.erp.order.dto;
import lombok.Builder; import lombok.Data;
import java.math.BigDecimal;
@Data @Builder
public class PurchaseOrderItemVO {
    Long id; Integer lineNo; Long productId; String productCode; String productName;
    String specification; BigDecimal quantity; String unit; BigDecimal unitPrice; BigDecimal totalPrice;
    Long relatedSalesItemId;
}