package com.erp.report.excel;
import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import lombok.Builder; import lombok.Data;
import java.math.BigDecimal;
/** 应收/应付账龄报表行 */
@Data @Builder
@ColumnWidth(16)
public class AgingRow {
    @ExcelProperty("类型") private String category;      // 应收/应付
    @ExcelProperty("单据编号") private String billNo;
    @ExcelProperty("往来单位") private String partner;
    @ExcelProperty("币种") private String currency;
    @ExcelProperty("总额") private BigDecimal totalAmount;
    @ExcelProperty("已结算") private BigDecimal settledAmount;
    @ExcelProperty("余额") private BigDecimal balance;
    @ExcelProperty("到期日") private String dueDate;
    @ExcelProperty("逾期天数") private Long overdueDays;
    @ExcelProperty("账龄区间") private String agingBucket;
    @ExcelProperty("状态") private String status;
}
