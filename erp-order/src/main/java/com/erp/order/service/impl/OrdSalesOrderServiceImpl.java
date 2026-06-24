package com.erp.order.service.impl;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.common.exception.BusinessException;
import com.erp.common.model.R;
import com.erp.order.dto.*;
import com.erp.order.entity.*;
import com.erp.order.mapper.*;
import com.erp.customer.mapper.CustCustomerMapper;
import com.erp.customer.entity.CustCustomer;
import com.erp.order.service.OrdSalesOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate; import java.time.format.DateTimeFormatter;
import java.util.List; import java.math.BigDecimal;; import java.util.stream.Collectors;

@Service @RequiredArgsConstructor
public class OrdSalesOrderServiceImpl implements OrdSalesOrderService {
    final OrdSalesOrderMapper orderMapper;
    final OrdSalesOrderItemMapper itemMapper;
    final OrdStatusHistoryMapper historyMapper;
    final CustCustomerMapper customerMapper;

    SalesOrderVO toVO(OrdSalesOrder o) {
        var items = itemMapper.selectList(new LambdaQueryWrapper<OrdSalesOrderItem>()
                .eq(OrdSalesOrderItem::getOrderId, o.getId()).orderByAsc(OrdSalesOrderItem::getLineNo));
        // 查询客户名称
        String custName = null;
        if (o.getCustomerId() != null) {
            var cust = customerMapper.selectById(o.getCustomerId());
            if (cust != null) custName = cust.getCustomerName();
        }
        return SalesOrderVO.builder().id(o.getId()).orderNo(o.getOrderNo())
            .customerId(o.getCustomerId()).customerName(custName).customerOrderNo(o.getCustomerOrderNo())
            .orderDate(o.getOrderDate()).currency(o.getCurrency())
            .tradeTerms(o.getTradeTerms()).paymentTerms(o.getPaymentTerms())
            .portLoading(o.getPortLoading()).portDestination(o.getPortDestination())
            .expectedDelivery(o.getExpectedDelivery())
            .totalAmount(o.getTotalAmount()).totalCnyAmount(o.getTotalCnyAmount())
            .exchangeRate(o.getExchangeRate()).remarks(o.getRemarks()).status(o.getStatus())
            .createdBy(o.getCreatedBy()).createdAt(o.getCreatedAt()).updatedAt(o.getUpdatedAt())
            .items(items.stream().map(this::toItemVO).toList()).build();
    }
    SalesOrderItemVO toItemVO(OrdSalesOrderItem i) {
        return SalesOrderItemVO.builder().id(i.getId()).lineNo(i.getLineNo())
            .productId(i.getProductId()).productCode(i.getProductCode()).productName(i.getProductName())
            .hsCode(i.getHsCode()).specification(i.getSpecification())
            .quantity(i.getQuantity()).unit(i.getUnit())
            .unitPrice(i.getUnitPrice()).totalPrice(i.getTotalPrice()).build();
    }

    public SalesOrderPageVO listPage(SalesOrderQuery q) {
        if (q.getPage()==null||q.getPage()<1) q.setPage(1);
        if (q.getSize()==null||q.getSize()<1) q.setSize(10);
        var w = new LambdaQueryWrapper<OrdSalesOrder>();
        if (q.getKeyword()!=null&&!q.getKeyword().isEmpty())
            w.like(OrdSalesOrder::getOrderNo,q.getKeyword());
        if (q.getCustomerId()!=null) w.eq(OrdSalesOrder::getCustomerId,q.getCustomerId());
        if (q.getStatus()!=null) w.eq(OrdSalesOrder::getStatus,q.getStatus());
        if (q.getDateFrom()!=null) w.ge(OrdSalesOrder::getOrderDate,q.getDateFrom());
        if (q.getDateTo()!=null) w.le(OrdSalesOrder::getOrderDate,q.getDateTo());
        w.orderByDesc(OrdSalesOrder::getCreatedAt);
        var p = orderMapper.selectPage(new Page<>(q.getPage(),Math.min(q.getSize(),100)), w);
        return SalesOrderPageVO.builder()
            .records(p.getRecords().stream().map(this::toVO).toList())
            .total(p.getTotal()).size(p.getSize()).current(p.getCurrent()).build();
    }

    public SalesOrderVO getById(Long id) {
        var o = orderMapper.selectById(id);
        if (o == null) throw new BusinessException(R.CODE_NOT_FOUND, "order not found");
        return toVO(o);
    }

    @Transactional public SalesOrderVO create(SalesOrderCreateRequest r, Long userId) {
        // 生成订单号 SO-20260625-XXX
        var dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        Long seq = orderMapper.selectCount(new LambdaQueryWrapper<OrdSalesOrder>()
                .like(OrdSalesOrder::getOrderNo, "SO-" + dateStr)) + 1;
        var o = new OrdSalesOrder();
        o.setOrderNo(String.format("SO-%s-%04d", dateStr, seq));
        o.setCustomerId(r.getCustomerId()); o.setCustomerOrderNo(r.getCustomerOrderNo());
        o.setOrderDate(r.getOrderDate()!=null?r.getOrderDate():LocalDate.now());
        o.setCurrency(r.getCurrency()!=null?r.getCurrency():"USD");
        o.setTradeTerms(r.getTradeTerms()); o.setPaymentTerms(r.getPaymentTerms());
        o.setPortLoading(r.getPortLoading()); o.setPortDestination(r.getPortDestination());
        o.setExpectedDelivery(r.getExpectedDelivery()); o.setRemarks(r.getRemarks());
        o.setExchangeRate(r.getExchangeRate());
        o.setStatus("draft"); o.setCreatedBy(userId);
        orderMapper.insert(o);

        // 明细
        BigDecimal exchg = r.getExchangeRate()!=null?r.getExchangeRate():new BigDecimal("7.25");
        BigDecimal total = BigDecimal.ZERO;
        if (r.getItems()!=null) for (int i=0;i<r.getItems().size();i++) {
            var ri = r.getItems().get(i);
            var item = new OrdSalesOrderItem();
            item.setOrderId(o.getId()); item.setLineNo(i+1);
            item.setProductId(ri.getProductId()); item.setProductCode(ri.getProductCode());
            item.setProductName(ri.getProductName()); item.setHsCode(ri.getHsCode());
            item.setSpecification(ri.getSpecification());
            item.setQuantity(ri.getQuantity()); item.setUnit(ri.getUnit()!=null?ri.getUnit():"件");
            item.setUnitPrice(ri.getUnitPrice());
            item.setTotalPrice(ri.getQuantity().multiply(ri.getUnitPrice()));
            total = total.add(item.getTotalPrice());
            itemMapper.insert(item);
        }
        o.setTotalAmount(total);
        o.setTotalCnyAmount(total.multiply(exchg));
        orderMapper.updateById(o);

        // 状态历史
        addHistory("sales", o.getId(), null, "draft", userId, "创建订单");
        return toVO(o);
    }

    @Transactional public void update(Long id, SalesOrderUpdateRequest r) {
        var o = orderMapper.selectById(id);
        if (o == null) throw new BusinessException(R.CODE_NOT_FOUND, "order not found");
        if (!"draft".equals(o.getStatus()))
            throw new BusinessException(R.CODE_PARAM_INVALID, "只能修改草稿状态的订单");
        if (r.getCustomerOrderNo()!=null) o.setCustomerOrderNo(r.getCustomerOrderNo());
        if (r.getExpectedDelivery()!=null) o.setExpectedDelivery(r.getExpectedDelivery());
        if (r.getPortLoading()!=null) o.setPortLoading(r.getPortLoading());
        if (r.getPortDestination()!=null) o.setPortDestination(r.getPortDestination());
        if (r.getPaymentTerms()!=null) o.setPaymentTerms(r.getPaymentTerms());
        if (r.getRemarks()!=null) o.setRemarks(r.getRemarks());
        if (r.getExchangeRate()!=null) { o.setExchangeRate(r.getExchangeRate());
            o.setTotalCnyAmount(o.getTotalAmount().multiply(r.getExchangeRate())); }
        orderMapper.updateById(o);
    }

    @Transactional public SalesOrderVO changeStatus(Long id, StatusChangeRequest r, Long userId) {
        var o = orderMapper.selectById(id);
        if (o == null) throw new BusinessException(R.CODE_NOT_FOUND, "order not found");
        var oldStatus = o.getStatus();
        o.setStatus(r.getToStatus());
        orderMapper.updateById(o);
        addHistory("sales", id, oldStatus, r.getToStatus(), userId, r.getRemark());
        return toVO(o);
    }

    void addHistory(String type, Long orderId, String from, String to, Long userId, String remark) {
        var h = new OrdStatusHistory();
        h.setOrderType(type); h.setOrderId(orderId);
        h.setFromStatus(from); h.setToStatus(to);
        h.setOperator(userId); h.setRemark(remark);
        historyMapper.insert(h);
    }

    @Transactional public void delete(Long id) {
        orderMapper.deleteById(id);
        itemMapper.delete(new LambdaQueryWrapper<OrdSalesOrderItem>().eq(OrdSalesOrderItem::getOrderId, id));
    }

    @Override
    public OrderProfitVO calculateProfit(Long orderId) {
        var o = orderMapper.selectById(orderId);
        if (o == null) throw new BusinessException(R.CODE_NOT_FOUND, "order not found");
        var items = itemMapper.selectList(new LambdaQueryWrapper<OrdSalesOrderItem>()
                .eq(OrdSalesOrderItem::getOrderId, orderId));
        var salesTotal = items.stream().map(OrdSalesOrderItem::getTotalPrice).reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
        var exchg = o.getExchangeRate() != null ? o.getExchangeRate() : java.math.BigDecimal.ONE;
        var salesCny = salesTotal.multiply(exchg);
        var purchaseTotal = salesTotal.multiply(new java.math.BigDecimal("0.6"));
        var profit = salesCny.subtract(purchaseTotal);
        var margin = salesCny.compareTo(java.math.BigDecimal.ZERO) > 0
                ? profit.multiply(new java.math.BigDecimal("100")).divide(salesCny, 2, java.math.RoundingMode.HALF_UP)
                : java.math.BigDecimal.ZERO;
        var detailItems = new java.util.ArrayList<OrderProfitVO.ProfitItemDetail>();
        for (var item : items) {
            var ip = item.getTotalPrice().multiply(new java.math.BigDecimal("0.6"));
            var ip2 = item.getTotalPrice().multiply(exchg).subtract(ip);
            detailItems.add(OrderProfitVO.ProfitItemDetail.builder()
                    .productName(item.getProductName()).salesTotal(item.getTotalPrice())
                    .purchaseTotal(ip).itemProfit(ip2)
                    .margin(item.getTotalPrice().compareTo(java.math.BigDecimal.ZERO) > 0
                        ? ip2.multiply(new java.math.BigDecimal("100")).divide(item.getTotalPrice().multiply(exchg), 2, java.math.RoundingMode.HALF_UP)
                        : java.math.BigDecimal.ZERO).build());
        }
        return OrderProfitVO.builder()
                .orderId(orderId).orderNo(o.getOrderNo())
                .totalSalesAmount(salesTotal).currency(o.getCurrency())
                .exchangeRate(exchg).totalSalesCny(salesCny)
                .totalPurchaseCost(purchaseTotal).estimatedProfit(profit).profitMargin(margin)
                .items(detailItems).build();
    }
}