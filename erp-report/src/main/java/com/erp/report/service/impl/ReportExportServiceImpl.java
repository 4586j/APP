package com.erp.report.service.impl;
import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.erp.common.storage.MinioTemplate;
import com.erp.finance.entity.FinExchangeRate;
import com.erp.finance.entity.FinPayable;
import com.erp.finance.entity.FinReceivable;
import com.erp.finance.mapper.FinExchangeRateMapper;
import com.erp.finance.mapper.FinPayableMapper;
import com.erp.finance.mapper.FinReceivableMapper;
import com.erp.order.entity.OrdPurchaseOrder;
import com.erp.order.entity.OrdSalesOrder;
import com.erp.order.mapper.OrdPurchaseOrderMapper;
import com.erp.order.mapper.OrdSalesOrderMapper;
import com.erp.report.dto.ReportExportRequest;
import com.erp.report.dto.ReportExportVO;
import com.erp.report.excel.AgingRow;
import com.erp.report.excel.ExchangeRateRow;
import com.erp.report.excel.OrderProfitRow;
import com.erp.report.service.ReportExportService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service @RequiredArgsConstructor
public class ReportExportServiceImpl implements ReportExportService {
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    final OrdSalesOrderMapper salesOrderMapper;
    final OrdPurchaseOrderMapper purchaseOrderMapper;
    final FinReceivableMapper receivableMapper;
    final FinPayableMapper payableMapper;
    final FinExchangeRateMapper exchangeRateMapper;
    final MinioTemplate minioTemplate;

    @Override
    public ReportExportVO exportOrderProfit(ReportExportRequest req) {
        var w = new LambdaQueryWrapper<OrdSalesOrder>().eq(OrdSalesOrder::getDeleted, 0);
        if (req.getStartDate() != null) w.ge(OrdSalesOrder::getOrderDate, req.getStartDate());
        if (req.getEndDate() != null) w.le(OrdSalesOrder::getOrderDate, req.getEndDate());
        w.orderByDesc(OrdSalesOrder::getOrderDate);
        var orders = salesOrderMapper.selectList(w);

        // 采购成本：按 related_sales_order_id 聚合采购单 total_amount（视为 CNY 成本）
        var pw = new LambdaQueryWrapper<OrdPurchaseOrder>().eq(OrdPurchaseOrder::getDeleted, 0);
        var purchases = purchaseOrderMapper.selectList(pw);
        Map<Long, BigDecimal> costBySalesOrder = purchases.stream()
            .filter(p -> p.getRelatedSalesOrderId() != null)
            .collect(Collectors.groupingBy(OrdPurchaseOrder::getRelatedSalesOrderId,
                Collectors.reducing(BigDecimal.ZERO, p -> nz(p.getTotalAmount()), BigDecimal::add)));

        List<OrderProfitRow> rows = new ArrayList<>();
        for (var o : orders) {
            var salesCny = nz(o.getTotalCnyAmount());
            var cost = costBySalesOrder.getOrDefault(o.getId(), BigDecimal.ZERO);
            var profit = salesCny.subtract(cost);
            var rate = salesCny.signum() == 0 ? BigDecimal.ZERO
                : profit.multiply(HUNDRED).divide(salesCny, 2, RoundingMode.HALF_UP);
            rows.add(OrderProfitRow.builder()
                .orderNo(o.getOrderNo()).orderDate(fmtDate(o.getOrderDate()))
                .customerId(o.getCustomerId()).currency(o.getCurrency())
                .salesAmount(nz(o.getTotalAmount())).salesCnyAmount(salesCny)
                .purchaseCost(cost).profit(profit).profitRate(rate)
                .status(o.getStatus()).build());
        }
        byte[] bytes = writeExcel(OrderProfitRow.class, rows, "销售订单利润");
        return store("order-profit", bytes, rows.size());
    }

    @Override
    public ReportExportVO exportAging(ReportExportRequest req) {
        var category = req.getCategory() == null ? "all" : req.getCategory();
        var today = LocalDate.now();
        List<AgingRow> rows = new ArrayList<>();

        if ("all".equals(category) || "receivable".equals(category)) {
            var rw = new LambdaQueryWrapper<FinReceivable>().eq(FinReceivable::getDeleted, 0);
            for (var r : receivableMapper.selectList(rw)) {
                var balance = nz(r.getTotalAmount()).subtract(nz(r.getReceivedAmount()));
                rows.add(AgingRow.builder().category("应收").billNo(r.getReceiptNo()).partner(r.getCustomerName())
                    .currency(r.getCurrency()).totalAmount(nz(r.getTotalAmount())).settledAmount(nz(r.getReceivedAmount()))
                    .balance(balance).dueDate(fmtDate(r.getDueDate())).overdueDays(overdue(r.getDueDate(), today))
                    .agingBucket(bucket(r.getDueDate(), today)).status(r.getStatus()).build());
            }
        }
        if ("all".equals(category) || "payable".equals(category)) {
            var pw = new LambdaQueryWrapper<FinPayable>().eq(FinPayable::getDeleted, 0);
            for (var p : payableMapper.selectList(pw)) {
                var balance = nz(p.getTotalAmount()).subtract(nz(p.getPaidAmount()));
                rows.add(AgingRow.builder().category("应付").billNo(p.getPayNo()).partner(p.getSupplierName())
                    .currency(p.getCurrency()).totalAmount(nz(p.getTotalAmount())).settledAmount(nz(p.getPaidAmount()))
                    .balance(balance).dueDate(fmtDate(p.getDueDate())).overdueDays(overdue(p.getDueDate(), today))
                    .agingBucket(bucket(p.getDueDate(), today)).status(p.getStatus()).build());
            }
        }
        byte[] bytes = writeExcel(AgingRow.class, rows, "账龄报表");
        return store("aging", bytes, rows.size());
    }

    @Override
    public ReportExportVO exportExchangeRate(ReportExportRequest req) {
        var w = new LambdaQueryWrapper<FinExchangeRate>().eq(FinExchangeRate::getDeleted, 0);
        if (req.getStartDate() != null) w.ge(FinExchangeRate::getRateDate, req.getStartDate());
        if (req.getEndDate() != null) w.le(FinExchangeRate::getRateDate, req.getEndDate());
        if (req.getCurrencyFrom() != null) w.eq(FinExchangeRate::getCurrencyFrom, req.getCurrencyFrom());
        if (req.getCurrencyTo() != null) w.eq(FinExchangeRate::getCurrencyTo, req.getCurrencyTo());
        w.orderByDesc(FinExchangeRate::getRateDate);
        var list = exchangeRateMapper.selectList(w);
        List<ExchangeRateRow> rows = list.stream().map(e -> ExchangeRateRow.builder()
            .currencyFrom(e.getCurrencyFrom()).currencyTo(e.getCurrencyTo())
            .rate(e.getRate()).rateDate(fmtDate(e.getRateDate())).source(e.getSource()).build()).toList();
        byte[] bytes = writeExcel(ExchangeRateRow.class, rows, "汇率历史");
        return store("exchange-rate", bytes, rows.size());
    }

    // ---------- helpers ----------

    private <T> byte[] writeExcel(Class<T> clazz, List<T> rows, String sheetName) {
        var out = new ByteArrayOutputStream();
        EasyExcel.write(out, clazz).sheet(sheetName).doWrite(rows);
        return out.toByteArray();
    }

    /** 写入 MinIO 并返回带预签名地址的结果 */
    private ReportExportVO store(String reportType, byte[] bytes, int rowCount) {
        var fileName = reportType + "-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)
            + "-" + System.currentTimeMillis() % 100000 + ".xlsx";
        var year = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy"));
        var objectKey = "report/" + reportType + "/" + year + "/" + fileName;
        minioTemplate.upload(objectKey, bytes,
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        var url = minioTemplate.presignedDownloadUrl(objectKey);
        return ReportExportVO.builder().fileName(fileName).objectKey(objectKey)
            .downloadUrl(url).fileSize(bytes.length).rowCount(rowCount).build();
    }

    private static BigDecimal nz(BigDecimal v) { return v == null ? BigDecimal.ZERO : v; }
    private static String fmtDate(LocalDate d) { return d == null ? "" : d.toString(); }

    private static Long overdue(LocalDate dueDate, LocalDate today) {
        if (dueDate == null) return 0L;
        long days = today.toEpochDay() - dueDate.toEpochDay();
        return Math.max(days, 0L);
    }

    /** 账龄区间：未到期 / 0-30 / 31-60 / 61-90 / 90+ */
    private static String bucket(LocalDate dueDate, LocalDate today) {
        long d = overdue(dueDate, today);
        if (dueDate == null) return "未知";
        if (d <= 0) return "未到期";
        if (d <= 30) return "0-30天";
        if (d <= 60) return "31-60天";
        if (d <= 90) return "61-90天";
        return "90天以上";
    }
}
