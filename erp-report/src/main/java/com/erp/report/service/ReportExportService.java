package com.erp.report.service;
import com.erp.report.dto.ReportExportRequest;
import com.erp.report.dto.ReportExportVO;
public interface ReportExportService {
    /** 销售订单利润报表导出 */
    ReportExportVO exportOrderProfit(ReportExportRequest req);
    /** 应收/应付账龄报表导出 */
    ReportExportVO exportAging(ReportExportRequest req);
    /** 汇率历史报表导出 */
    ReportExportVO exportExchangeRate(ReportExportRequest req);
}
