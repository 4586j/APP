package com.erp.report.dto;
import lombok.Data;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
/** 报表导出通用请求（按日期范围筛选） */
@Data
public class ReportExportRequest {
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startDate;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate endDate;
    /** 账龄报表用：receivable/payable/all，默认 all */
    private String category;
    /** 汇率报表用：可选币种过滤 */
    private String currencyFrom;
    private String currencyTo;
}
