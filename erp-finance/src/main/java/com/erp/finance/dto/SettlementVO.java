package com.erp.finance.dto;
import lombok.Builder; import lombok.Data;
import java.math.BigDecimal; import java.time.LocalDateTime;
@Data @Builder
public class SettlementVO {
    Long id; String settlementNo; String direction; Long receivableId; Long payableId;
    String relatedType; Long relatedId; BigDecimal amount; String paymentMethod;
    String bankAccount; String description; Long settledBy; LocalDateTime settledAt; LocalDateTime createdAt;
}