package com.erp.product.dto;
import lombok.Builder; import lombok.Data;
import java.math.BigDecimal; import java.time.LocalDateTime;
@Data @Builder
public class HsCodeVO {
    Long id; String hsCode; String description;
    BigDecimal tariffRate; BigDecimal vatRate; BigDecimal exportRefundRate;
    String restrictions; LocalDateTime createdAt; LocalDateTime updatedAt;
}