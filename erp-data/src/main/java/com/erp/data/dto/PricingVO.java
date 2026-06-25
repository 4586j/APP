package com.erp.data.dto;
import lombok.Data; import java.math.BigDecimal; import java.time.LocalDateTime;
@Data public class PricingVO {
    private Long id; private Long productId; private String productName;
    private String title; private BigDecimal costPrice; private BigDecimal targetPrice;
    private BigDecimal competitorPrice; private BigDecimal suggestedPrice;
    private BigDecimal margin; private String marketTrend;
    private String analysisData; private String status; private String remark;
    private Long createdBy; private LocalDateTime createdAt; private LocalDateTime updatedAt;
}