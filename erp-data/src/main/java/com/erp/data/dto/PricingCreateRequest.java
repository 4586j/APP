package com.erp.data.dto;
import jakarta.validation.constraints.NotBlank; import jakarta.validation.constraints.NotNull;
import lombok.Data; import java.math.BigDecimal;
@Data public class PricingCreateRequest {
    @NotNull private Long productId; @NotBlank private String title;
    private BigDecimal costPrice; private BigDecimal targetPrice;
    private BigDecimal competitorPrice; private BigDecimal suggestedPrice;
    private BigDecimal margin; private String marketTrend;
    private String analysisData; private String status; private String remark;
}