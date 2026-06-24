package com.erp.finance.dto;
import jakarta.validation.constraints.NotBlank; import jakarta.validation.constraints.NotNull;
import lombok.Data; import java.math.BigDecimal;
@Data
public class FundApprovalCreateRequest {
    @NotBlank String title; @NotBlank String fundType;
    @NotNull BigDecimal amount; String currency="CNY";
    Long supplierId; String description;
}