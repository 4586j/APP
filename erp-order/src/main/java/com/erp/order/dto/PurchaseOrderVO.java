package com.erp.order.dto;
import lombok.Builder; import lombok.Data;
import java.math.BigDecimal; import java.time.LocalDate; import java.time.LocalDateTime;
import java.util.List;
@Data @Builder
public class PurchaseOrderVO {
    Long id; String orderNo; Long supplierId; String supplierName; Long relatedSalesOrderId;
    LocalDate orderDate; LocalDate expectedDelivery;
    BigDecimal totalAmount; String currency; String paymentTerms;
    String remarks; String status; List<PurchaseOrderItemVO> items;
    LocalDateTime createdAt; LocalDateTime updatedAt;
}