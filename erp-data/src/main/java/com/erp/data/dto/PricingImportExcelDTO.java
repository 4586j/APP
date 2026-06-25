package com.erp.data.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;
import java.math.BigDecimal;

/**
 * 定价分析批量导入 Excel 模板 DTO。
 */
@Data
public class PricingImportExcelDTO {

    @ExcelProperty("产品ID")
    private Long productId;

    @ExcelProperty("分析标题")
    private String title;

    @ExcelProperty("成本价")
    private BigDecimal costPrice;

    @ExcelProperty("目标价")
    private BigDecimal targetPrice;

    @ExcelProperty("竞品价")
    private BigDecimal competitorPrice;

    @ExcelProperty("建议售价")
    private BigDecimal suggestedPrice;

    @ExcelProperty("利润率(%)")
    private BigDecimal margin;

    @ExcelProperty("市场趋势")
    private String marketTrend;

    @ExcelProperty("状态")
    private String status;

    @ExcelProperty("备注")
    private String remark;
}
