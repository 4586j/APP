package com.erp.finance.dto;
import lombok.Builder; import lombok.Data;
import java.math.BigDecimal; import java.util.List;
/** 账龄统计结果 */
@Data @Builder
public class AgingReportVO {
    /** 账龄分桶（未到期 / 1-30 / 31-60 / 61-90 / 90+） */
    List<Bucket> buckets;
    BigDecimal totalOutstanding;

    @Data @Builder
    public static class Bucket {
        String label;     // 未到期 / 1-30天 / 31-60天 / 61-90天 / 90天以上
        int minDays;      // 含
        int maxDays;      // 不含；-1 表示无上限
        long count;       // 单据数
        BigDecimal amount;// 未结清金额合计
    }
}
