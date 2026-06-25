package com.erp.finance.service.impl;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.finance.dto.*;
import com.erp.finance.entity.FinReceivable;
import com.erp.finance.mapper.FinReceivableMapper;
import com.erp.finance.service.FinReceivableService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate; import java.time.format.DateTimeFormatter;

@Service @RequiredArgsConstructor
public class FinReceivableServiceImpl implements FinReceivableService {
    final FinReceivableMapper mapper;
    @Override public ReceivablePageVO listPage(ReceivableQuery q){
        if(q.getPage()==null||q.getPage()<1)q.setPage(1); if(q.getSize()==null||q.getSize()<1)q.setSize(20);
        var w=new LambdaQueryWrapper<FinReceivable>().eq(FinReceivable::getDeleted,0);
        if(q.getStatus()!=null)w.eq(FinReceivable::getStatus,q.getStatus());
        if(q.getCustomerId()!=null)w.eq(FinReceivable::getCustomerId,q.getCustomerId());
        if(q.getKeyword()!=null)w.like(FinReceivable::getReceiptNo,"%"+q.getKeyword()+"%").or().like(FinReceivable::getCustomerName,"%"+q.getKeyword()+"%");
        w.orderByDesc(FinReceivable::getCreatedAt);
        var p=mapper.selectPage(new Page<>(q.getPage(),Math.min(q.getSize(),100)),w);
        var records=p.getRecords().stream().map(e->ReceivableVO.builder().id(e.getId()).receiptNo(e.getReceiptNo())
            .sourceType(e.getSourceType()).sourceId(e.getSourceId()).customerId(e.getCustomerId()).customerName(e.getCustomerName())
            .totalAmount(e.getTotalAmount()).receivedAmount(e.getReceivedAmount())
            .balance(e.getTotalAmount().subtract(e.getReceivedAmount()!=null?e.getReceivedAmount():java.math.BigDecimal.ZERO))
            .dueDate(e.getDueDate()).currency(e.getCurrency()).status(e.getStatus()).remark(e.getRemark()).createdAt(e.getCreatedAt()).build()).toList();
        return ReceivablePageVO.builder().records(records).total(p.getTotal()).size(p.getSize()).current(p.getCurrent()).build();
    }
    @Override public ReceivableVO getById(Long id){
        var e=mapper.selectById(id); if(e==null)return null;
        return ReceivableVO.builder().id(e.getId()).receiptNo(e.getReceiptNo()).sourceType(e.getSourceType()).sourceId(e.getSourceId())
            .customerId(e.getCustomerId()).customerName(e.getCustomerName()).totalAmount(e.getTotalAmount()).receivedAmount(e.getReceivedAmount())
            .balance(e.getTotalAmount().subtract(e.getReceivedAmount()!=null?e.getReceivedAmount():java.math.BigDecimal.ZERO))
            .dueDate(e.getDueDate()).currency(e.getCurrency()).status(e.getStatus()).remark(e.getRemark()).createdAt(e.getCreatedAt()).build();
    }
    @Override @Transactional public Long create(ReceivableCreateRequest r){
        var datePart=LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        var e=new FinReceivable(); e.setReceiptNo("REC-"+datePart+"-"+System.currentTimeMillis()%100000);
        e.setSourceType(r.getSourceType()); e.setSourceId(r.getSourceId());
        e.setCustomerId(r.getCustomerId()); e.setCustomerName(r.getCustomerName());
        e.setTotalAmount(r.getTotalAmount()); e.setDueDate(r.getDueDate()); e.setRemark(r.getRemark());
        mapper.insert(e); return e.getId();
    }
    @Override @Transactional public void delete(Long id){mapper.deleteById(id);}
}