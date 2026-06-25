package com.erp.report.excel;
import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import lombok.Builder; import lombok.Data;
import java.math.BigDecimal;
/** 汇率历史报表行 */
@Data @Builder
@ColumnWidth(16)
public class ExchangeRateRow {
    @ExcelProperty("原币种") private String currencyFrom;
    @ExcelProperty("目标币种") private String currencyTo;
    @ExcelProperty("汇率") private BigDecimal rate;
    @ExcelProperty("汇率日期") private String rateDate;
    @ExcelProperty("来源") private String source;
}
