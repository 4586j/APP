package com.erp.finance.dto;
import lombok.Builder; import lombok.Data;
import java.math.BigDecimal; import java.time.LocalDate; import java.time.LocalDateTime;
@Data @Builder
public class PayableVO {
    Long id; String payNo; String sourceType; Long sourceId; Long supplierId; String supplierName;
    BigDecimal totalAmount; BigDecimal paidAmount; BigDecimal balance;
    LocalDate dueDate; String currency; String status; String remark; LocalDateTime createdAt;
}