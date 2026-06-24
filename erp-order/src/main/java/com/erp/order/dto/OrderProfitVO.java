package com.erp.order.dto;
import lombok.Builder; import lombok.Data;
import java.math.BigDecimal; import java.util.List;
@Data @Builder
public class OrderProfitVO {
    Long orderId; String orderNo;
    BigDecimal totalSalesAmount; String currency;
    BigDecimal exchangeRate; BigDecimal totalSalesCny;
    BigDecimal totalPurchaseCost; BigDecimal estimatedProfit;
    BigDecimal profitMargin;
    List<ProfitItemDetail> items;
    @Data @Builder
    public static class ProfitItemDetail {
        String productName; BigDecimal salesTotal; BigDecimal purchaseTotal;
        BigDecimal itemProfit; BigDecimal margin;
    }
}
