package com.erp.report.controller;
import com.erp.common.model.R;
import com.erp.report.dto.ReportExportRequest;
import com.erp.report.dto.ReportExportVO;
import com.erp.report.service.ReportExportService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/v1/reports/export") @RequiredArgsConstructor
public class ReportExportController {
    final ReportExportService service;

    /** 销售订单利润报表导出（按日期范围），返回 MinIO 下载链接 */
    @PostMapping("/order-profit")
    @PreAuthorize("hasAuthority('report:export')")
    public R<ReportExportVO> orderProfit(@RequestBody(required=false) ReportExportRequest req){
        return R.ok(service.exportOrderProfit(req==null?new ReportExportRequest():req));
    }

    /** 应收/应付账龄报表导出 */
    @PostMapping("/aging")
    @PreAuthorize("hasAuthority('report:export')")
    public R<ReportExportVO> aging(@RequestBody(required=false) ReportExportRequest req){
        return R.ok(service.exportAging(req==null?new ReportExportRequest():req));
    }

    /** 汇率历史报表导出 */
    @PostMapping("/exchange-rate")
    @PreAuthorize("hasAuthority('report:export')")
    public R<ReportExportVO> exchangeRate(@RequestBody(required=false) ReportExportRequest req){
        return R.ok(service.exportExchangeRate(req==null?new ReportExportRequest():req));
    }
}
