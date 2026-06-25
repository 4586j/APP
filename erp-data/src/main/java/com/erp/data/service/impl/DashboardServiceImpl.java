package com.erp.data.service.impl;
import com.erp.data.dto.DashboardStatsVO;
import com.erp.data.dto.DashboardStatsVO.MonthlyTrend;
import com.erp.data.dto.DashboardStatsVO.OrderStatusDist;
import com.erp.data.service.DashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
@Slf4j @Service @RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {
    final JdbcTemplate jdbc;
    @Override public DashboardStatsVO getStats() {
        DashboardStatsVO vo = new DashboardStatsVO();
        vo.setCustomerCount(safeCount("crm_customer"));
        vo.setProductCount(safeCount("prd_product"));
        vo.setOrderCount(safeCount("ord_sales_order"));
        try {
            BigDecimal rev = jdbc.queryForObject(
                "SELECT COALESCE(SUM(total_amount),0) FROM ord_sales_order WHERE deleted=0 AND DATE_FORMAT(created_at,'%Y-%m')=DATE_FORMAT(NOW(),'%Y-%m')",
                BigDecimal.class);
            vo.setMonthlyRevenue(rev != null ? rev : BigDecimal.ZERO);
        } catch (DataAccessException e) {
            log.warn("Dashboard: ord_sales_order not available, revenue=0");
            vo.setMonthlyRevenue(BigDecimal.ZERO);
        }
        vo.setMonthlyProfit(vo.getMonthlyRevenue().multiply(BigDecimal.valueOf(0.4)));
        try {
            List<MonthlyTrend> trend = jdbc.query(
                "SELECT DATE_FORMAT(created_at,'%Y-%m') as month, COALESCE(SUM(total_amount),0) as revenue FROM ord_sales_order WHERE deleted=0 AND created_at >= DATE_SUB(NOW(), INTERVAL 12 MONTH) GROUP BY DATE_FORMAT(created_at,'%Y-%m') ORDER BY month",
                (rs, row) -> {
                    MonthlyTrend t = new MonthlyTrend();
                    t.setMonth(rs.getString("month")); t.setRevenue(rs.getBigDecimal("revenue"));
                    t.setProfit(rs.getBigDecimal("revenue").multiply(BigDecimal.valueOf(0.4))); return t;
                });
            vo.setTrend(trend);
        } catch (DataAccessException e) {
            log.warn("Dashboard: trend data unavailable");
            vo.setTrend(new ArrayList<>());
        }
        try {
            List<OrderStatusDist> dist = jdbc.query(
                "SELECT status, COUNT(*) as cnt FROM ord_sales_order WHERE deleted=0 GROUP BY status",
                (rs, row) -> {
                    OrderStatusDist d = new OrderStatusDist();
                    d.setName(rs.getString("status")); d.setValue(rs.getLong("cnt")); return d;
                });
            vo.setOrderStatusDist(dist);
        } catch (DataAccessException e) {
            log.warn("Dashboard: status distribution unavailable");
            vo.setOrderStatusDist(new ArrayList<>());
        }
        return vo;
    }
    private Long safeCount(String table) {
        try {
            Long cnt = jdbc.queryForObject("SELECT COUNT(*) FROM " + table + " WHERE deleted=0", Long.class);
            return cnt != null ? cnt : 0L;
        } catch (DataAccessException e) {
            log.warn("Dashboard: table {} not available, count=0", table);
            return 0L;
        }
    }
}