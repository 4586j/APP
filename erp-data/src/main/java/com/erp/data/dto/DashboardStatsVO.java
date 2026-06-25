package com.erp.data.dto;
import lombok.Data; import java.math.BigDecimal; import java.util.List;
@Data public class DashboardStatsVO {
    private long customerCount; private long productCount; private long orderCount;
    private BigDecimal monthlyRevenue; private BigDecimal monthlyProfit;
    private List<MonthlyTrend> trend; private List<OrderStatusDist> orderStatusDist;
    @Data public static class MonthlyTrend {
        private String month; private BigDecimal revenue; private BigDecimal profit;
    }
    @Data public static class OrderStatusDist {
        private String name; private long value;
    }
}