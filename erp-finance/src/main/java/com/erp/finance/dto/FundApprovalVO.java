package com.erp.finance.dto;
import lombok.Builder; import lombok.Data;
import java.math.BigDecimal; import java.time.LocalDateTime;
@Data @Builder
public class FundApprovalVO {
    Long id; String requestNo; String title; String fundType;
    BigDecimal amount; String currency; Long supplierId; String supplierName;
    String description; String status; Long applicant; LocalDateTime createdAt;
}