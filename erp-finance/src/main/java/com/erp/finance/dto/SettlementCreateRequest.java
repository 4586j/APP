package com.erp.finance.dto;
import jakarta.validation.constraints.*; import lombok.Data;
import java.math.BigDecimal; import java.time.LocalDateTime;
@Data
public class SettlementCreateRequest {
    @NotBlank String direction; Long receivableId; Long payableId;
    String relatedType; Long relatedId;
    @NotNull BigDecimal amount; String paymentMethod; String bankAccount;
    String description; Long settledBy; LocalDateTime settledAt;
}