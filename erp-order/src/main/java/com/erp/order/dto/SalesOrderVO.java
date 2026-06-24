package com.erp.order.dto;
import lombok.Builder; import lombok.Data;
import java.math.BigDecimal; import java.time.LocalDate; import java.time.LocalDateTime;
import java.util.List;
@Data @Builder
public class SalesOrderVO {
    Long id; String orderNo; Long customerId; String customerName; String customerOrderNo;
    LocalDate orderDate; String currency; String tradeTerms; String paymentTerms;
    String portLoading; String portDestination; LocalDate expectedDelivery;
    BigDecimal totalAmount; BigDecimal totalCnyAmount; BigDecimal exchangeRate;
    String remarks; String status; List<SalesOrderItemVO> items;
    Long createdBy; LocalDateTime createdAt; LocalDateTime updatedAt;
}