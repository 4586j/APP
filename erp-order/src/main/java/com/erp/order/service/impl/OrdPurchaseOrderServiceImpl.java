package com.erp.order.service.impl;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.common.exception.BusinessException;
import com.erp.common.model.R;
import com.erp.order.dto.*;
import com.erp.order.entity.*;
import com.erp.order.mapper.*;
import com.erp.order.service.OrdPurchaseOrderService;
import com.erp.order.service.impl.OrdSalesOrderServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate; import java.time.format.DateTimeFormatter;
import java.math.BigDecimal;

@Service @RequiredArgsConstructor
public class OrdPurchaseOrderServiceImpl implements OrdPurchaseOrderService {
    final OrdPurchaseOrderMapper orderMapper;
    final OrdPurchaseOrderItemMapper itemMapper;
    final OrdSalesOrderServiceImpl salesOrderService; // 复用时注入

    PurchaseOrderVO toVO(OrdPurchaseOrder o) {
        var items = itemMapper.selectList(new LambdaQueryWrapper<OrdPurchaseOrderItem>()
                .eq(OrdPurchaseOrderItem::getOrderId, o.getId()).orderByAsc(OrdPurchaseOrderItem::getLineNo));
        return PurchaseOrderVO.builder().id(o.getId()).orderNo(o.getOrderNo())
            .supplierId(o.getSupplierId()).relatedSalesOrderId(o.getRelatedSalesOrderId())
            .orderDate(o.getOrderDate()).expectedDelivery(o.getExpectedDelivery())
            .totalAmount(o.getTotalAmount()).currency(o.getCurrency())
            .paymentTerms(o.getPaymentTerms()).remarks(o.getRemarks()).status(o.getStatus())
            .createdAt(o.getCreatedAt()).updatedAt(o.getUpdatedAt())
            .items(items.stream().map(i->PurchaseOrderItemVO.builder().id(i.getId()).lineNo(i.getLineNo())
                .productId(i.getProductId()).productCode(i.getProductCode()).productName(i.getProductName())
                .specification(i.getSpecification()).quantity(i.getQuantity()).unit(i.getUnit())
                .unitPrice(i.getUnitPrice()).totalPrice(i.getTotalPrice())
                .relatedSalesItemId(i.getRelatedSalesItemId()).build()).toList()).build();
    }

    public PurchaseOrderPageVO listPage(PurchaseOrderQuery q) {
        if (q.getPage()==null||q.getPage()<1) q.setPage(1);
        if (q.getSize()==null||q.getSize()<1) q.setSize(10);
        var w = new LambdaQueryWrapper<OrdPurchaseOrder>();
        if (q.getKeyword()!=null&&!q.getKeyword().isEmpty()) w.like(OrdPurchaseOrder::getOrderNo,q.getKeyword());
        if (q.getSupplierId()!=null) w.eq(OrdPurchaseOrder::getSupplierId,q.getSupplierId());
        if (q.getStatus()!=null) w.eq(OrdPurchaseOrder::getStatus,q.getStatus());
        w.orderByDesc(OrdPurchaseOrder::getCreatedAt);
        var p = orderMapper.selectPage(new Page<>(q.getPage(),Math.min(q.getSize(),100)), w);
        return PurchaseOrderPageVO.builder()
            .records(p.getRecords().stream().map(this::toVO).toList())
            .total(p.getTotal()).size(p.getSize()).current(p.getCurrent()).build();
    }

    public PurchaseOrderVO getById(Long id) {
        var o = orderMapper.selectById(id);
        if (o==null) throw new BusinessException(R.CODE_NOT_FOUND,"purchase order not found");
        return toVO(o);
    }

    @Transactional public PurchaseOrderVO create(PurchaseOrderCreateRequest r, Long userId) {
        var dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        Long seq = orderMapper.selectCount(new LambdaQueryWrapper<OrdPurchaseOrder>()
                .like(OrdPurchaseOrder::getOrderNo, "PO-" + dateStr)) + 1;
        var o = new OrdPurchaseOrder();
        o.setOrderNo(String.format("PO-%s-%04d", dateStr, seq));
        o.setSupplierId(r.getSupplierId()); o.setRelatedSalesOrderId(r.getRelatedSalesOrderId());
        o.setOrderDate(r.getOrderDate()!=null?r.getOrderDate():LocalDate.now());
        o.setExpectedDelivery(r.getExpectedDelivery());
        o.setCurrency(r.getCurrency()!=null?r.getCurrency():"CNY");
        o.setPaymentTerms(r.getPaymentTerms()); o.setRemarks(r.getRemarks());
        o.setStatus("draft");
        orderMapper.insert(o);

        BigDecimal total = BigDecimal.ZERO;
        if (r.getItems()!=null) for (int i=0;i<r.getItems().size();i++) {
            var ri = r.getItems().get(i);
            var item = new OrdPurchaseOrderItem();
            item.setOrderId(o.getId()); item.setLineNo(i+1);
            item.setProductId(ri.getProductId()); item.setProductCode(ri.getProductCode());
            item.setProductName(ri.getProductName()); item.setSpecification(ri.getSpecification());
            item.setQuantity(ri.getQuantity()); item.setUnit(ri.getUnit()!=null?ri.getUnit():"件");
            item.setUnitPrice(ri.getUnitPrice());
            item.setTotalPrice(ri.getQuantity().multiply(ri.getUnitPrice()));
            item.setRelatedSalesItemId(ri.getRelatedSalesItemId());
            total = total.add(item.getTotalPrice());
            itemMapper.insert(item);
        }
        o.setTotalAmount(total);
        orderMapper.updateById(o);

        salesOrderService.addHistory("purchase", o.getId(), null, "draft", userId, "创建采购订单");
        return toVO(o);
    }

    @Transactional public void updateStatus(Long id, StatusChangeRequest r, Long userId) {
        var o = orderMapper.selectById(id);
        if (o==null) throw new BusinessException(R.CODE_NOT_FOUND,"purchase order not found");
        var old = o.getStatus(); o.setStatus(r.getToStatus());
        orderMapper.updateById(o);
        salesOrderService.addHistory("purchase", id, old, r.getToStatus(), userId, r.getRemark());
    }

    @Transactional public void delete(Long id) {
        orderMapper.deleteById(id);
        itemMapper.delete(new LambdaQueryWrapper<OrdPurchaseOrderItem>().eq(OrdPurchaseOrderItem::getOrderId, id));
    }
}