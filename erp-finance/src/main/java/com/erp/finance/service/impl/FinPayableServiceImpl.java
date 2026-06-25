package com.erp.finance.service.impl;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.finance.dto.*;
import com.erp.finance.entity.FinPayable;
import com.erp.finance.mapper.FinPayableMapper;
import com.erp.finance.service.FinPayableService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate; import java.time.format.DateTimeFormatter;

@Service @RequiredArgsConstructor
public class FinPayableServiceImpl implements FinPayableService {
    final FinPayableMapper mapper;
    @Override public PayablePageVO listPage(PayableQuery q){
        if(q.getPage()==null||q.getPage()<1)q.setPage(1); if(q.getSize()==null||q.getSize()<1)q.setSize(20);
        var w=new LambdaQueryWrapper<FinPayable>().eq(FinPayable::getDeleted,0);
        if(q.getStatus()!=null)w.eq(FinPayable::getStatus,q.getStatus());
        if(q.getSupplierId()!=null)w.eq(FinPayable::getSupplierId,q.getSupplierId());
        if(q.getKeyword()!=null)w.like(FinPayable::getPayNo,"%"+q.getKeyword()+"%").or().like(FinPayable::getSupplierName,"%"+q.getKeyword()+"%");
        w.orderByDesc(FinPayable::getCreatedAt);
        var p=mapper.selectPage(new Page<>(q.getPage(),Math.min(q.getSize(),100)),w);
        var records=p.getRecords().stream().map(e->PayableVO.builder().id(e.getId()).payNo(e.getPayNo())
            .sourceType(e.getSourceType()).sourceId(e.getSourceId()).supplierId(e.getSupplierId()).supplierName(e.getSupplierName())
            .totalAmount(e.getTotalAmount()).paidAmount(e.getPaidAmount())
            .balance(e.getTotalAmount().subtract(e.getPaidAmount()!=null?e.getPaidAmount():java.math.BigDecimal.ZERO))
            .dueDate(e.getDueDate()).currency(e.getCurrency()).status(e.getStatus()).remark(e.getRemark()).createdAt(e.getCreatedAt()).build()).toList();
        return PayablePageVO.builder().records(records).total(p.getTotal()).size(p.getSize()).current(p.getCurrent()).build();
    }
    @Override public PayableVO getById(Long id){
        var e=mapper.selectById(id); if(e==null)return null;
        return PayableVO.builder().id(e.getId()).payNo(e.getPayNo()).sourceType(e.getSourceType()).sourceId(e.getSourceId())
            .supplierId(e.getSupplierId()).supplierName(e.getSupplierName()).totalAmount(e.getTotalAmount()).paidAmount(e.getPaidAmount())
            .balance(e.getTotalAmount().subtract(e.getPaidAmount()!=null?e.getPaidAmount():java.math.BigDecimal.ZERO))
            .dueDate(e.getDueDate()).currency(e.getCurrency()).status(e.getStatus()).remark(e.getRemark()).createdAt(e.getCreatedAt()).build();
    }
    @Override @Transactional public Long create(PayableCreateRequest r){
        var datePart=LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        var e=new FinPayable(); e.setPayNo("PAY-"+datePart+"-"+System.currentTimeMillis()%100000);
        e.setSourceType(r.getSourceType()); e.setSourceId(r.getSourceId());
        e.setSupplierId(r.getSupplierId()); e.setSupplierName(r.getSupplierName());
        e.setTotalAmount(r.getTotalAmount()); e.setDueDate(r.getDueDate()); e.setRemark(r.getRemark());
        mapper.insert(e); return e.getId();
    }
    @Override @Transactional public void delete(Long id){mapper.deleteById(id);}
}