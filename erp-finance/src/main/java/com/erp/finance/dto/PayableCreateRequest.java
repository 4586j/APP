package com.erp.finance.dto;
import jakarta.validation.constraints.*; import lombok.Data;
import java.math.BigDecimal; import java.time.LocalDate;
@Data
public class PayableCreateRequest {
    @NotBlank String sourceType; @NotNull Long sourceId; Long supplierId; String supplierName;
    @NotNull BigDecimal totalAmount; LocalDate dueDate; String remark;
}