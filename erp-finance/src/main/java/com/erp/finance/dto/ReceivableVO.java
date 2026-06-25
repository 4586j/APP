package com.erp.finance.dto;
import lombok.Builder; import lombok.Data;
import java.math.BigDecimal; import java.time.LocalDate; import java.time.LocalDateTime;
@Data @Builder
public class ReceivableVO {
    Long id; String receiptNo; String sourceType; Long sourceId; Long customerId; String customerName;
    BigDecimal totalAmount; BigDecimal receivedAmount; BigDecimal balance;
    LocalDate dueDate; String currency; String status; String remark; LocalDateTime createdAt;
}