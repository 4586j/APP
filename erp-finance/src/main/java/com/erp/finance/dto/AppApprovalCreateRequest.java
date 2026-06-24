package com.erp.finance.dto;
import jakarta.validation.constraints.NotBlank; import jakarta.validation.constraints.NotNull;
import lombok.Data; import java.math.BigDecimal;
@Data
public class AppApprovalCreateRequest {
    @NotBlank String targetType; @NotNull Long targetId;
    @NotBlank String title; @NotNull BigDecimal amount;
}