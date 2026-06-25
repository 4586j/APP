package com.erp.finance.service.impl;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.finance.dto.*;
import com.erp.finance.entity.*;
import com.erp.finance.mapper.*;
import com.erp.finance.service.FinSettlementService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal; import java.time.LocalDate; import java.time.LocalDateTime; import java.time.format.DateTimeFormatter;

@Service @RequiredArgsConstructor
public class FinSettlementServiceImpl implements FinSettlementService {
    final FinSettlementMapper mapper;
    final FinReceivableMapper receivableMapper;
    final FinPayableMapper payableMapper;

    @Override public SettlementPageVO listPage(SettlementQuery q){
        if(q.getPage()==null||q.getPage()<1)q.setPage(1); if(q.getSize()==null||q.getSize()<1)q.setSize(20);
        var w=new LambdaQueryWrapper<FinSettlement>();
        if(q.getDirection()!=null)w.eq(FinSettlement::getDirection,q.getDirection());
        if(q.getReceivableId()!=null)w.eq(FinSettlement::getReceivableId,q.getReceivableId());
        if(q.getPayableId()!=null)w.eq(FinSettlement::getPayableId,q.getPayableId());
        w.orderByDesc(FinSettlement::getCreatedAt);
        var p=mapper.selectPage(new Page<>(q.getPage(),Math.min(q.getSize(),100)),w);
        var records=p.getRecords().stream().map(e->SettlementVO.builder().id(e.getId()).settlementNo(e.getSettlementNo())
            .direction(e.getDirection()).receivableId(e.getReceivableId()).payableId(e.getPayableId())
            .relatedType(e.getRelatedType()).relatedId(e.getRelatedId()).amount(e.getAmount())
            .paymentMethod(e.getPaymentMethod()).bankAccount(e.getBankAccount()).description(e.getDescription())
            .settledBy(e.getSettledBy()).settledAt(e.getSettledAt()).createdAt(e.getCreatedAt()).build()).toList();
        return SettlementPageVO.builder().records(records).total(p.getTotal()).size(p.getSize()).current(p.getCurrent()).build();
    }
    @Override public SettlementVO getById(Long id){
        var e=mapper.selectById(id); if(e==null)return null;
        return SettlementVO.builder().id(e.getId()).settlementNo(e.getSettlementNo()).direction(e.getDirection())
            .receivableId(e.getReceivableId()).payableId(e.getPayableId()).relatedType(e.getRelatedType()).relatedId(e.getRelatedId())
            .amount(e.getAmount()).paymentMethod(e.getPaymentMethod()).bankAccount(e.getBankAccount()).description(e.getDescription())
            .settledBy(e.getSettledBy()).settledAt(e.getSettledAt()).createdAt(e.getCreatedAt()).build();
    }
    @Override @Transactional
    public Long create(SettlementCreateRequest r){
        var datePart=LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        var e=new FinSettlement(); e.setSettlementNo("SET-"+datePart+"-"+System.currentTimeMillis()%100000);
        e.setDirection(r.getDirection()); e.setReceivableId(r.getReceivableId()); e.setPayableId(r.getPayableId());
        e.setRelatedType(r.getRelatedType()); e.setRelatedId(r.getRelatedId()); e.setAmount(r.getAmount());
        e.setPaymentMethod(r.getPaymentMethod()!=null?r.getPaymentMethod():"bank_transfer"); e.setBankAccount(r.getBankAccount());
        e.setDescription(r.getDescription()); e.setSettledBy(r.getSettledBy()); e.setSettledAt(r.getSettledAt()!=null?r.getSettledAt():LocalDateTime.now());
        mapper.insert(e);
        // 更新应收/应付已收/已付金额
        if("receipt".equals(r.getDirection()) && r.getReceivableId()!=null){
            var rec=receivableMapper.selectById(r.getReceivableId());
            if(rec!=null){
                rec.setReceivedAmount(rec.getReceivedAmount()!=null?rec.getReceivedAmount().add(r.getAmount()):r.getAmount());
                BigDecimal bal=rec.getTotalAmount().subtract(rec.getReceivedAmount());
                rec.setStatus(bal.compareTo(BigDecimal.ZERO)<=0?"received":"partially");
                receivableMapper.updateById(rec);
            }
        }
        if("payment".equals(r.getDirection()) && r.getPayableId()!=null){
            var pay=payableMapper.selectById(r.getPayableId());
            if(pay!=null){
                pay.setPaidAmount(pay.getPaidAmount()!=null?pay.getPaidAmount().add(r.getAmount()):r.getAmount());
                BigDecimal bal=pay.getTotalAmount().subtract(pay.getPaidAmount());
                pay.setStatus(bal.compareTo(BigDecimal.ZERO)<=0?"paid":"partially");
                payableMapper.updateById(pay);
            }
        }
        return e.getId();
    }
    @Override @Transactional public void delete(Long id){mapper.deleteById(id);}
}