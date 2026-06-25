package com.erp.data.entity;
import com.erp.common.base.BaseEntity;
import lombok.Data; import lombok.EqualsAndHashCode;
import java.math.BigDecimal;
@Data @EqualsAndHashCode(callSuper=true)
public class DatPricingAnalysis extends BaseEntity {
    private Long productId; private String title;
    private BigDecimal costPrice; private BigDecimal targetPrice;
    private BigDecimal competitorPrice; private BigDecimal suggestedPrice;
    private BigDecimal margin; private String marketTrend;
    private String analysisData; private String status; private String remark;
}