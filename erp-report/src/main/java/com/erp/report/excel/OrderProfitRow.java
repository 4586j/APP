package com.erp.report.excel;
import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import lombok.Builder; import lombok.Data;
import java.math.BigDecimal;
/** 销售订单利润报表行 */
@Data @Builder
@ColumnWidth(18)
public class OrderProfitRow {
    @ExcelProperty("订单号") private String orderNo;
    @ExcelProperty("下单日期") private String orderDate;
    @ExcelProperty("客户ID") private Long customerId;
    @ExcelProperty("币种") private String currency;
    @ExcelProperty("销售额(原币)") private BigDecimal salesAmount;
    @ExcelProperty("销售额(CNY)") private BigDecimal salesCnyAmount;
    @ExcelProperty("采购成本(CNY)") private BigDecimal purchaseCost;
    @ExcelProperty("毛利(CNY)") private BigDecimal profit;
    @ExcelProperty("毛利率%") private BigDecimal profitRate;
    @ExcelProperty("状态") private String status;
}
